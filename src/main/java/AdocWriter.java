import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class AdocWriter {
    private static final String DEFAULT_FILENAME = "output.adoc";
    private final StringBuilder adocContent;
    private String title;
    private String author;
    private boolean includeToc;
    private final Scanner scanner;

    public AdocWriter() {
        adocContent = new StringBuilder();
        scanner = new Scanner(System.in);
    }

    public void start() {
        System.out.println("Welcome to AdocWriter - AsciiDoc File Generator");
        System.out.println("--------------------------------------------");

        collectMetadata();
        generateHeader();
        writeContent();
        saveDocument();
        scanner.close();
    }

    private void collectMetadata() {
        System.out.print("Enter document title: ");
        title = scanner.nextLine().trim();

        System.out.print("Enter author name: ");
        author = scanner.nextLine().trim();

        System.out.print("Include table of contents? (y/n): ");
        includeToc = scanner.nextLine().trim().toLowerCase().startsWith("y");
    }

    private void generateHeader() {
        adocContent.append("= ").append(title).append("\n");
        adocContent.append(author).append("\n");
        adocContent.append(":doctype: article\n");
        adocContent.append(":encoding: utf-8\n");
        adocContent.append(":lang: en\n");
        adocContent.append(":source-highlighter: highlightjs\n");

        if (includeToc) {
            adocContent.append(":toc: left\n");
            adocContent.append(":toclevels: 3\n");
        }

        adocContent.append("\n\n");
    }

    private void writeContent() {
        System.out.println("\nStart writing your content:");
        System.out.println("Use -l to create a bullet point line");
        System.out.println("Use -c to create a collapsible section (provide title after -c)");
        System.out.println("Use -t to create a table");
        System.out.println("Use -code [language] to start a code snippet");
        System.out.println("Use = [level] [text] to create a heading (e.g. '= 2 Section Title' for level 2 heading)");
        System.out.println("Type 'exit' to finish and save the document");

        String currentCollapsibleSection = null;
        StringBuilder collapsibleContent = new StringBuilder();
        boolean inCodeSnippet = false;
        StringBuilder codeContent = new StringBuilder();
        String codeLanguage = "";

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase("exit")) {
                if (currentCollapsibleSection != null) {
                    addCollapsibleSection(currentCollapsibleSection, collapsibleContent.toString());
                }
                if (inCodeSnippet) {
                    addCodeSnippet(codeLanguage, codeContent.toString());
                }
                break;
            }

            if (input.equals("end-code") && inCodeSnippet) {
                if (currentCollapsibleSection != null) {
                    collapsibleContent.append(addCodeSnippetToString(codeLanguage, codeContent.toString()));
                } else {
                    addCodeSnippet(codeLanguage, codeContent.toString());
                }
                inCodeSnippet = false;
                codeContent = new StringBuilder();
                codeLanguage = "";
                System.out.println("Code snippet ended");
                continue;
            }

            if (inCodeSnippet) {
                codeContent.append(input).append("\n");
                continue;
            }

            if (input.startsWith("-l ")) {
                String content = input.substring(3);

                if (currentCollapsibleSection != null) {
                    collapsibleContent.append("* ").append(content).append("\n");
                } else {
                    adocContent.append("* ").append(content).append("\n\n");
                }
            } else if (input.startsWith("= ")) {
                try {
                    String[] parts = input.split(" ", 3);
                    if (parts.length >= 3) {
                        int level = Integer.parseInt(parts[1]);
                        String headingText = parts[2];

                        if (level < 1 || level > 6) {
                            System.out.println("Heading level must be between 1 and 6");
                            continue;
                        }

                        String heading = createHeading(level, headingText);

                        if (currentCollapsibleSection != null) {
                            collapsibleContent.append(heading);
                        } else {
                            adocContent.append(heading);
                        }
                    } else {
                        System.out.println("Invalid heading format. Use '= [level] [text]'");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid heading level. Use '= [level] [text]'");
                }
            } else if (input.startsWith("-c ")) {
                if (currentCollapsibleSection != null) {
                    addCollapsibleSection(currentCollapsibleSection, collapsibleContent.toString());
                    collapsibleContent = new StringBuilder();
                }

                currentCollapsibleSection = input.substring(3);
                System.out.println("Writing collapsible section: " + currentCollapsibleSection);
                System.out.println("(Add content with -l, start new section with -c, or type 'end-c' to end this section)");
            } else if (input.equals("end-c") && currentCollapsibleSection != null) {
                addCollapsibleSection(currentCollapsibleSection, collapsibleContent.toString());
                collapsibleContent = new StringBuilder();
                currentCollapsibleSection = null;
                System.out.println("Collapsible section ended");
            } else if (input.startsWith("-code ")) {
                codeLanguage = input.substring(6).trim();
                inCodeSnippet = true;
                System.out.println("Writing code snippet in " + codeLanguage);
                System.out.println("Enter code lines. Type 'end-code' when finished.");
            } else if (input.equals("-t")) {
                if (currentCollapsibleSection != null) {
                    collapsibleContent.append(createTable());
                } else {
                    adocContent.append(createTable());
                }
            } else {
                System.out.println("Invalid command. Available commands: -l, -c, -code, -t, = [level] [text], end-c, end-code, exit");
            }
        }
    }

    private String createHeading(int level, String text) {
        return "=".repeat(Math.max(0, level)) +
                " " + text + "\n\n";
    }

    private String createTable() {
        List<String> headers = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();

        System.out.println("Table Creation:");
        System.out.print("Enter number of columns: ");
        int columns = Integer.parseInt(scanner.nextLine());

        System.out.println("Enter column headers:");
        for (int i = 0; i < columns; i++) {
            System.out.print("Header " + (i + 1) + ": ");
            headers.add(scanner.nextLine());
        }

        System.out.println("Enter table data (type 'end-table' when finished):");
        do {
            List<String> row = new ArrayList<>();
            System.out.println("Row " + (rows.size() + 1) + ":");

            for (int i = 0; i < columns; i++) {
                System.out.print(headers.get(i) + ": ");
                String cell = scanner.nextLine();

                if (cell.equals("end-table")) {
                    if (i == 0 && row.isEmpty()) {
                        return formatTable(headers, rows);
                    } else {
                        System.out.println("Please complete the current row or start a new row before ending the table.");
                        i--;
                    }
                } else {
                    row.add(cell);
                }
            }

            rows.add(row);
            System.out.print("Add another row? (y/n): ");
        } while (scanner.nextLine().trim().toLowerCase().startsWith("y"));

        return formatTable(headers, rows);
    }

    private String formatTable(List<String> headers, List<List<String>> rows) {
        StringBuilder tableBuilder = new StringBuilder();

        tableBuilder.append("|===\n");

        tableBuilder.append("|");
        tableBuilder.append(String.join(" |", headers));
        tableBuilder.append("\n\n");

        for (List<String> row : rows) {
            tableBuilder.append("|");
            tableBuilder.append(String.join(" |", row));
            tableBuilder.append("\n\n");
        }
        tableBuilder.append("|===\n\n");

        return tableBuilder.toString();
    }

    private void addCodeSnippet(String language, String content) {
        adocContent.append(addCodeSnippetToString(language, content));
    }

    private String addCodeSnippetToString(String language, String content) {
        return "[source," + language + "]\n" +
                "----\n" +
                content +
                "----\n\n";
    }

    private void addCollapsibleSection(String title, String content) {
        adocContent.append(".").append(title).append("\n");
        adocContent.append("[%collapsible]\n");
        adocContent.append("====\n");
        adocContent.append(content);
        adocContent.append("====\n\n");
    }

    private void saveDocument() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(DEFAULT_FILENAME))) {
            writer.write(adocContent.toString());
            System.out.println("\nDocument saved as '" + DEFAULT_FILENAME + "'");
        } catch (IOException e) {
            System.err.println("Error saving document: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        AdocWriter adocWriter = new AdocWriter();
        adocWriter.start();
    }
}