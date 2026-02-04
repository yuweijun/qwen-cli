package com.example.askquery.service;

import com.example.askquery.config.AppProperties;
import com.example.askquery.config.DashscopeProperties;
import com.example.askquery.model.HistoryEntry;
import com.fasterxml.jackson.databind.JsonNode;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class InteractiveService {

    private final AppProperties appProps;
    private final DashscopeProperties dashProps;
    private final DashscopeClient client;
    private final HistoryManager historyManager;

    private final Deque<Map<String, String>> convo = new ArrayDeque<>();
    private final List<Entry> entries = new ArrayList<>();
    private final ExecutorService executor;
    private final boolean parallelMode;

    public InteractiveService(AppProperties appProps, DashscopeProperties dashProps, DashscopeClient client) {
        this.appProps = appProps;
        this.dashProps = dashProps;
        this.client = client;
        this.historyManager = new HistoryManager(appProps.getHistoryFile(), 100); // Keep latest 100 records
        this.parallelMode = appProps.isParallel();
        int threads = Math.max(1, appProps.getConcurrency());
        this.executor = parallelMode ? Executors.newFixedThreadPool(threads) : null;

        // Load existing history entries
        loadHistoryEntries();
    }

    private static class Entry {
        final String question;
        String answer;

        Entry(String q) {
            this.question = q;
            this.answer = null;
        }

        void setAnswer(String a) {
            this.answer = a;
        }
    }

    /**
     * Loads history entries from the JSON file into the entries list
     */
    private void loadHistoryEntries() {
        List<HistoryEntry> historyEntries = historyManager.loadHistory();
        synchronized (entries) {
            entries.clear();
            for (HistoryEntry historyEntry : historyEntries) {
                Entry entry = new Entry(historyEntry.getQuestion());
                entry.setAnswer(historyEntry.getAnswer());
                entries.add(entry);
            }
        }
    }

    public void run(String initialQuery) throws Exception {
        String jsonHistPath = appProps.getHistoryFile();

        // For JLine history, use a separate text file in tmp directory with timestamp to avoid conflicts with JSON format
        String timestamp = String.valueOf(System.currentTimeMillis());
        String jlineHistPath = System.getProperty("java.io.tmpdir") + File.separator + ".qwen_jline_history_" + timestamp + ".txt";

        ensureHistoryFile(jlineHistPath);

        Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .build();

        DefaultHistory history = new DefaultHistory();

        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .parser(new DefaultParser())
                .variable(LineReader.HISTORY_FILE, Paths.get(jlineHistPath))
                .history(history)
                .build();

        // Load JLine history from its file
        try {
            history.attach(reader);
            history.load();
        } catch (Exception e) {
            // If JLine history loading fails, continue without it
            System.err.println("Warning: Could not load JLine history file: " + e.getMessage());
        }

        // Load questions from JSON history and add unique ones to JLine history
        try {
            List<HistoryEntry> jsonHistory = historyManager.loadHistory();
            for (HistoryEntry entry : jsonHistory) {
                // Add to JLine history only if it's not already there
                // Check if the question is already in the current history
                boolean exists = false;
                for (org.jline.reader.History.Entry histEntry : history) {
                    if (entry.getQuestion().equals(histEntry.line())) {
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    history.add(entry.getQuestion());
                }
            }
        } catch (Exception e) {
            // If JSON history loading fails, continue without it
            System.err.println("Warning: Could not load JSON history: " + e.getMessage());
        }

        List<String> exits = Arrays.stream(appProps.getExitCommands().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (reader instanceof LineReaderImpl) {
                    ((LineReaderImpl) reader).getHistory().save();
                }
            } catch (Exception ignored) {
            }
            if (executor != null) {
                executor.shutdown();
                try {
                    executor.awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                }
            }
        }));

        if (initialQuery != null && !initialQuery.isBlank()) {
            // Add to JLine history as well as file
            reader.getHistory().add(initialQuery);
            appendHistoryUnique(jlineHistPath, initialQuery);
            submitAndMaybeWait(initialQuery, jsonHistPath, reader, exits);
        }

        String prompt = "\n========================================================" +
                        "\n请输入你的问题（或输入 exit/quit 退出；h 查看历史记录）： ";
        while (true) {
            String line;
            try {
                line = reader.readLine(prompt);
            } catch (UserInterruptException e) {
                System.out.println("\n已收到 Ctrl+C，退出并等待进行中的请求...");
                break;
            } catch (EndOfFileException e) {
                System.out.println("\nEOF，退出...");
                break;
            }

            if (line == null) {
                break;
            }

            String s = line.trim();
            if (s.isEmpty()) continue;

            if (s.equalsIgnoreCase("h") || s.equalsIgnoreCase(":h") || s.equalsIgnoreCase(":history")) {
                showHistory(terminal);
                continue;
            }

            if (exits.contains(s.toLowerCase())) {
                System.out.println("收到退出命令，等待进行中的请求并退出...");
                break;
            }

            // Add to JLine history as well as file
            System.out.println("========================================================\n");
            reader.getHistory().add(s);
            appendHistoryUnique(jlineHistPath, s);
            submitAndMaybeWait(s, jsonHistPath, reader, exits);
        }

        if (executor != null) {
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);
        }

        try {
            history.save();
        } catch (Exception ignored) {
        }
    }


    private void submitAndMaybeWait(String query, String histPath, LineReader reader, List<String> exits) {
        // First, check if the question already exists in history
        HistoryEntry existingEntry = findExistingQuestion(query);

        if (existingEntry != null) {
            // Use existing answer from history
            if (!parallelMode) {
                useExistingAnswer(query, existingEntry.getAnswer());
            } else {
                // For parallel mode, submit to executor to maintain consistency
                executor.submit(() -> useExistingAnswer(query, existingEntry.getAnswer()));
            }
            return;
        }

        // If not found in history, proceed with the normal flow
        synchronized (convo) {
            convo.addLast(Map.of("role", "user", "content", query));
            if (appProps.getContextLength() > 0) {
                while (convo.size() > appProps.getContextLength() * 2) convo.removeFirst();
            }
        }

        final Entry entry = new Entry(query);
        synchronized (entries) {
            entries.add(entry);
        }

        if (!parallelMode) {
            List<Map<String, String>> msgs = buildMessages(query);
            JsonNode resp = client.sendMessages(msgs, dashProps.getModel());
            String text = client.extractText(resp);
            synchronized (convo) {
                convo.addLast(Map.of("role", "assistant", "content", text));
                if (appProps.getContextLength() > 0) {
                    while (convo.size() > appProps.getContextLength() * 2) convo.removeFirst();
                }
            }
            entry.setAnswer(text);
            System.out.println("[回答] " + text);

            // Add to JSON history
            historyManager.addEntry(new HistoryEntry(query, text));

            // Save question and response to file
            saveQuestionToFile(query, text);
        } else {
            executor.submit(() -> {
                List<Map<String, String>> msgs = buildMessages(query);
                JsonNode resp = client.sendMessages(msgs, dashProps.getModel());
                String text = client.extractText(resp);
                synchronized (convo) {
                    convo.addLast(Map.of("role", "assistant", "content", text));
                    if (appProps.getContextLength() > 0) {
                        while (convo.size() > appProps.getContextLength() * 2) convo.removeFirst();
                    }
                }
                entry.setAnswer(text);
                synchronized (System.out) {
                    System.out.println("\n[回答] " + text);

                    // Add to JSON history
                    historyManager.addEntry(new HistoryEntry(query, text));

                    // Save question and response to file
                    saveQuestionToFile(query, text);
                }
            });
        }
    }

    /**
     * Finds an existing question in the history and returns its entry
     */
    private HistoryEntry findExistingQuestion(String query) {
        List<HistoryEntry> historyEntries = historyManager.loadHistory();

        // Look for an exact match of the question
        for (HistoryEntry entry : historyEntries) {
            if (entry.getQuestion().equals(query) && entry.getAnswer() != null) {
                return entry;
            }
        }

        return null; // Not found
    }

    /**
     * Uses an existing answer from history
     */
    private void useExistingAnswer(String question, String answer) {
        // Add to conversation context if needed
        synchronized (convo) {
            convo.addLast(Map.of("role", "user", "content", question));
            convo.addLast(Map.of("role", "assistant", "content", answer));
            if (appProps.getContextLength() > 0) {
                while (convo.size() > appProps.getContextLength() * 2) convo.removeFirst();
            }
        }

        // Create and add entry to in-memory list
        Entry entry = new Entry(question);
        entry.setAnswer(answer);
        synchronized (entries) {
            entries.add(entry);
        }

        // Print the existing answer
        if (parallelMode) {
            synchronized (System.out) {
                System.out.println("[回答] " + answer);
                System.out.println("(从历史记录中获取的答案)");
            }
        } else {
            System.out.println("[回答] " + answer);
            System.out.println("(从历史记录中获取的答案)");
        }

        // Note: We don't need to save to file again since it's already in history
    }

    private List<Map<String, String>> buildMessages(String currentUser) {
        List<Map<String, String>> msgs = new ArrayList<>();
        if (appProps.getSystemMessage() != null && !appProps.getSystemMessage().isBlank()) {
            msgs.add(Map.of("role", "system", "content", appProps.getSystemMessage()));
        }
        if (appProps.getContextLength() > 0) {
            synchronized (convo) {
                for (Map<String, String> m : convo) {
                    msgs.add(Map.of("role", m.get("role"), "content", m.get("content")));
                }
            }
        }
        msgs.add(Map.of("role", "user", "content", currentUser));
        return msgs;
    }

    private void ensureHistoryFile(String path) {
        try {
            Path p = Paths.get(path);
            if (!Files.exists(p)) {
                Path parent = p.getParent();
                if (parent != null && !Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
                Files.createFile(p);
            }
        } catch (Exception ignored) {
        }
    }


    private void showHistory(Terminal terminal) {
        // Load history from JSON file
        List<HistoryEntry> historyEntries = historyManager.loadHistory();

        // Clear screen first to create a clean view
        for (int i = 0; i < 50; i++) {
            terminal.writer().println();  // Print multiple newlines to clear screen
        }
        terminal.flush();

        if (historyEntries.isEmpty()) {
            terminal.writer().println("没有历史记录。");
            terminal.flush();
            return;
        }

        terminal.writer().println("========== 历史记录 ==========");
        for (int i = 0; i < historyEntries.size(); i++) {
            HistoryEntry entry = historyEntries.get(i);
            terminal.writer().println("[" + (i + 1) + "] 问题: \u001B[32m" + entry.getQuestion() + "\u001B[0m");
            if (entry.getAnswer() != null) {
                terminal.writer().println("    回答: " + entry.getAnswer());
            } else {
                terminal.writer().println("    回答: (等待中...)");
            }
            terminal.writer().println("---------------------------");

            // Add five blank lines between questions (except for the last entry)
            if (i < historyEntries.size() - 1) {
                for (int j = 0; j < 2; j++) {
                    terminal.writer().println();
                }
            }
        }
        terminal.writer().println("按 q 键或 Esc 键退出历史记录视图...");
        terminal.flush();

        // Save original terminal attributes
        Attributes originalAttrs = terminal.enterRawMode(); // ✅ This is the correct way!

        try {
            int ch;
            while (true) {
                ch = terminal.reader().read();
                if (ch == 'q' || ch == 'Q' || ch == 27) { // 27 = ESC
                    break;
                }
                if (ch == 3 || ch == 4) { // Ctrl+C or Ctrl+D
                    break;
                }
            }
        } catch (IOException e) {
            // Ignore
        } finally {
            // Restore original terminal mode
            terminal.setAttributes(originalAttrs);
        }

        // Clear screen when exiting to return to clean prompt
        terminal.writer().println("\n".repeat(50));
        terminal.flush();
    }


    private void appendHistoryUnique(String historyFile, String line) {
        try {
            Path p = Paths.get(historyFile);
            if (!Files.exists(p)) {
                ensureHistoryFile(historyFile);
            }
            List<String> lines = Files.readAllLines(p);
            String last = lines.isEmpty() ? null : lines.get(lines.size() - 1);
            if (last != null && last.equals(line)) return;
            Files.write(p, Collections.singletonList(line), StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }

    private void saveQuestionToFile(String question, String response) {
        try {
            // Get current date in yyyy-MM-dd format
            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            // Extract first 35 characters of the question and sanitize for filename
            String first35Chars = question.length() > 35 ? question.substring(0, 35) : question;
            String sanitized = first35Chars.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "_");

            // Create the filename with .html extension for styled content
            String fileName = date + "_" + sanitized + ".html";

            // Create the content with Monokai-themed markdown
            String content = createMonokaiStyledMarkdown(question, response);

            // Write to file in questions directory
            Path questionsDir = Paths.get("questions");
            if (!Files.exists(questionsDir)) {
                Files.createDirectories(questionsDir);
            }

            Path filePath = questionsDir.resolve(fileName);
            Files.write(filePath, content.getBytes());

            System.out.println("Question saved to " + filePath.toAbsolutePath() + " (HTML with Monokai theme)");
        } catch (IOException e) {
            System.err.println("Error saving question to file: " + e.getMessage());
        }
    }

    /**
     * Creates markdown content with Monokai color scheme styling
     */
    private String createMonokaiStyledMarkdown(String question, String response) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <title>%s</title>
                    <style>
                        body {
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            background-color: #272822;
                            color: #f8f8f2;
                            margin: 0;
                            padding: 20px;
                            line-height: 1.6;
                        }
                        .container {
                            max-width: 800px;
                            margin: 0 auto;
                        }
                        h1 {
                            color: #a6e22e;
                            border-bottom: 2px solid #a6e22e;
                            padding-bottom: 10px;
                        }
                        h2 {
                            color: #66d9ef;
                        }
                        h3 {
                            color: #fd971f;
                        }
                        h4, h5, h6 {
                            color: #ae81ff;
                        }
                        code {
                            background-color: #3c3d38;
                            color: #fd971f;
                            padding: 2px 4px;
                            border-radius: 3px;
                            font-family: 'Consolas', 'Courier New', monospace;
                        }
                        pre {
                            background-color: #2d2d2d;
                            border-left: 4px solid #a6e22e;
                            padding: 15px;
                            overflow-x: auto;
                            border-radius: 4px;
                        }
                        pre code {
                            background-color: transparent;
                            color: inherit;
                            padding: 0;
                        }
                        blockquote {
                            border-left: 4px solid #75715e;
                            margin: 0;
                            padding: 0 15px;
                            color: #75715e;
                        }
                        ul, ol {
                            padding-left: 20px;
                        }
                        li {
                            margin: 5px 0;
                        }
                        a {
                            color: #66d9ef;
                            text-decoration: none;
                        }
                        a:hover {
                            text-decoration: underline;
                        }
                        table {
                            border-collapse: collapse;
                            width: 100%%;
                            margin: 15px 0;
                        }
                        th, td {
                            border: 1px solid #444;
                            padding: 8px 12px;
                            text-align: left;
                        }
                        th {
                            background-color: #3c3d38;
                            color: #a6e22e;
                        }
                        tr:nth-child(even) {
                            background-color: #2d2d2d;
                        }
                        .question {
                            background-color: #3c3d38;
                            padding: 15px;
                            border-radius: 5px;
                            margin-bottom: 20px;
                            border-left: 4px solid #a6e22e;
                        }
                        .response {
                            padding: 10px 0;
                        }
                        .timestamp {
                            color: #75715e;
                            font-size: 0.9em;
                            text-align: right;
                            margin-top: 20px;
                            font-style: italic;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="question">
                            <h1>❓ %s</h1>
                        </div>
                        
                        <div class="response">
                            %s
                        </div>
                        
                        <div class="timestamp">
                            Generated on %s
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                    escapeHtml(question),
                    escapeHtml(question),
                    convertMarkdownToHtml(response),
                    java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                );
    }

    /**
     * Convert markdown to HTML using flexmark library with extensions
     */
    private String convertMarkdownToHtml(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }

        // Configure flexmark with extensions
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, Arrays.asList(
                TablesExtension.create(),
                AutolinkExtension.create(),
                StrikethroughExtension.create(),
                TaskListExtension.create()
        ));
        
        // Configure parser options
        options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");
        options.set(TablesExtension.COLUMN_SPANS, false);
        options.set(TablesExtension.MIN_HEADER_ROWS, 1);
        options.set(TablesExtension.MAX_HEADER_ROWS, 1);
        options.set(TablesExtension.APPEND_MISSING_COLUMNS, true);
        options.set(TablesExtension.DISCARD_EXTRA_COLUMNS, true);
        options.set(TablesExtension.HEADER_SEPARATOR_COLUMN_MATCH, true);

        // Create parser and renderer
        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        // Parse and render
        return renderer.render(parser.parse(markdown));
    }

    /**
     * Escape HTML special characters
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#x27;");
    }
}