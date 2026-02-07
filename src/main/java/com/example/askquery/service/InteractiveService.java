package com.example.askquery.service;

import com.example.askquery.config.AppProperties;
import com.example.askquery.config.DashscopeProperties;
import com.example.askquery.model.HistoryEntry;
import com.example.askquery.util.BatRenderer;
import com.example.askquery.util.MarkdownRenderer;
import com.example.askquery.util.AnsiColors;
import com.example.askquery.util.FilenameUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.jetbrains.annotations.NotNull;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.LineReaderImpl;
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
    private final SearchHistoryService searchHistoryService;

    private final Deque<Map<String, String>> convo = new ArrayDeque<>();
    private final List<Entry> entries = new ArrayList<>();
    private final ExecutorService executor;
    private final boolean parallelMode;
    private final List<String> exits;

    public InteractiveService(AppProperties appProps, DashscopeProperties dashProps, DashscopeClient client) {
        this.appProps = appProps;
        this.dashProps = dashProps;
        this.client = client;
        this.historyManager = new HistoryManager(appProps.getHistoryFile(), 100); // Keep latest 100 records
        this.parallelMode = appProps.isParallel();
        int threads = Math.max(1, appProps.getConcurrency());
        this.executor = parallelMode ? Executors.newFixedThreadPool(threads) : null;

        // Initialize bat command path from configuration
        BatRenderer.setBatCommand(appProps.getBatCommand());

        // Initialize search history service
        this.searchHistoryService = new SearchHistoryService(this.historyManager, this.appProps);
        
        // Initialize exit commands list
        this.exits = Arrays.stream(appProps.getExitCommands().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toList());

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
     * Check if the input is a command that shouldn't be added to history
     * @param input the user input to check
     * @return true if input is a command, false otherwise
     */
    public boolean isCommand(String input) {
        return input.equalsIgnoreCase("h") || 
               input.equalsIgnoreCase(":h") || 
               input.equalsIgnoreCase(":history") ||
               input.equalsIgnoreCase("s") || 
               input.equalsIgnoreCase(":s") || 
               input.equalsIgnoreCase(":search") ||
               input.equalsIgnoreCase("o") ||
               exits.contains(input.toLowerCase());
    }
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

        // For JLine history, use a separate text file in tmp directory with timestamp 
        // to avoid conflicts with JSON format
        String timestamp = String.valueOf(System.currentTimeMillis());
        String jlineHistPath = System.getProperty("java.io.tmpdir") + File.separator + 
            ".qwen_jline_history_" + timestamp + ".txt";

        ensureHistoryFile(jlineHistPath);

        Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .build();

        CommandFilteringHistory history = new CommandFilteringHistory(this);

        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .parser(new DefaultParser())
                .variable(LineReader.HISTORY_FILE, Paths.get(jlineHistPath))
                .history(history)
                .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
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
            // Add to file (custom history handles JLine history)
            appendHistoryUnique(jlineHistPath, initialQuery);
            submitAndMaybeWait(initialQuery, jsonHistPath, reader, exits);
        }

        String prompt = "\n" + AnsiColors.promptHeader("请输入你的问题") +
                        AnsiColors.promptText("（或输入 ") + 
                        AnsiColors.promptNavigation("'q'") +
                        AnsiColors.promptText(" 退出；") +
                        AnsiColors.promptNavigation("'h'") +
                        AnsiColors.promptText(" 查看历史记录；") +
                        AnsiColors.promptNavigation("'s'") +
                        AnsiColors.promptText(" 搜索历史记录；") +
                        AnsiColors.promptNavigation("'o'") +
                        AnsiColors.promptText(" 在浏览器中打开上次的回答）： ");
        while (true) {
            String line;
            try {
                line = reader.readLine(prompt);
            } catch (UserInterruptException e) {
                System.out.println("\n" + AnsiColors.promptInfo("已收到 Ctrl+C，") + AnsiColors.promptNavigation("退出并等待进行中的请求..."));
                break;
            } catch (EndOfFileException e) {
                System.out.println("\n" + AnsiColors.promptNavigation("EOF，退出..."));
                break;
            }

            if (line == null) {
                break;
            }

            String s = line.trim();
            if (s.isEmpty()) continue;

            // Check if this is a command that shouldn't be added to history
            if (isCommand(s)) {
                if (s.equalsIgnoreCase("h") || s.equalsIgnoreCase(":h") || s.equalsIgnoreCase(":history")) {
                    searchHistoryService.displayLatestHistory();
                } else if (s.equalsIgnoreCase("s") || s.equalsIgnoreCase(":s") || s.equalsIgnoreCase(":search")) {
                    searchHistoryService.handleSearchInteraction();
                } else if (s.equalsIgnoreCase("o")) {
                    openLastResponseInBrowser();
                } else if (exits.contains(s.toLowerCase())) {
                    System.out.println(AnsiColors.promptInfo("收到退出命令，") + AnsiColors.promptNavigation("等待进行中的请求并退出..."));
                    break;
                }
                continue;
            }

            // Add to file (custom history handles JLine history)
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
            
            // Render response using bat if available and configured, otherwise use plain text
            if (appProps.isUseBatRendering() && BatRenderer.isBatAvailable()) {
                System.out.println("[回答]");
                if (!BatRenderer.renderToTerminal(text, appProps.getBatTheme())) {
                    // Fallback to plain text if bat rendering fails
                    System.out.println(text);
                }
            } else {
                System.out.println("[回答] " + text);
            }

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
                    // Render response using bat if available and configured, otherwise use plain text
                    if (appProps.isUseBatRendering() && BatRenderer.isBatAvailable()) {
                        System.out.println("\n[回答]");
                        if (!BatRenderer.renderToTerminal(text, appProps.getBatTheme())) {
                            // Fallback to plain text if bat rendering fails
                            System.out.println(text);
                        }
                    } else {
                        System.out.println("\n[回答] " + text);
                    }

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
                // Render response using bat if available and configured, otherwise use plain text
                if (appProps.isUseBatRendering() && BatRenderer.isBatAvailable()) {
                    System.out.println("[回答]");
                    if (!BatRenderer.renderToTerminal(answer, appProps.getBatTheme())) {
                        // Fallback to plain text if bat rendering fails
                        System.out.println(answer);
                    }
                } else {
                    System.out.println("[回答] " + answer);
                }
                System.out.println("(从历史记录中获取的答案)");
            }
        } else {
            // Render response using bat if available and configured, otherwise use plain text
            if (appProps.isUseBatRendering() && BatRenderer.isBatAvailable()) {
                System.out.println("[回答]");
                if (!BatRenderer.renderToTerminal(answer, appProps.getBatTheme())) {
                    // Fallback to plain text if bat rendering fails
                    System.out.println(answer);
                }
            } else {
                System.out.println("[回答] " + answer);
            }
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

    private void openLastResponseInBrowser() {
        synchronized (entries) {
            if (entries.isEmpty()) {
                System.out.println(AnsiColors.promptInfo("还没有任何问答记录。"));
                return;
            }

            // Get the last entry
            Entry lastEntry = entries.get(entries.size() - 1);
            
            if (lastEntry.answer == null || lastEntry.answer.isEmpty()) {
                System.out.println(AnsiColors.promptInfo("最后一个问题的回答尚未生成。"));
                return;
            }

            try {
                // Get current date in yyyy-MM-dd format
                String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

                String sanitized = FilenameUtils.sanitizeFilename(lastEntry.question);

                // Create the filename with .html extension for styled content
                String fileName = date + "_" + sanitized + ".html";

                // Create the content with Monokai-themed markdown
                String content = MarkdownRenderer.createMonokaiStyledMarkdown(lastEntry.question, lastEntry.answer);

                // Write to file in questions directory
                Path questionsDir = Paths.get("questions");
                if (!Files.exists(questionsDir)) {
                    Files.createDirectories(questionsDir);
                }

                Path filePath = questionsDir.resolve(fileName);
                Files.write(filePath, content.getBytes());

                // Open in browser
                // Try different browsers/commands based on OS
                String os = System.getProperty("os.name").toLowerCase();
                Process process;
                
                if (os.contains("mac")) {
                    process = Runtime.getRuntime().exec(new String[]{"open", filePath.toString()});
                } else if (os.contains("win")) {
                    process = Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", "\"\"", filePath.toString()});
                } else {
                    // Linux and other Unix-like systems
                    process = Runtime.getRuntime().exec(new String[]{"xdg-open", filePath.toString()});
                }
                
                process.waitFor();
                System.out.println(AnsiColors.promptInfo("已在浏览器中打开回答：") + filePath.toAbsolutePath());
            } catch (Exception e) {
                System.err.println(AnsiColors.promptError("打开浏览器失败：") + e.getMessage());
            }
        }
    }

    private void saveQuestionToFile(String question, String response) {
        try {
            // Get current date in yyyy-MM-dd format
            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            // Extract first 35 characters of the question and sanitize for filename
            String sanitized = FilenameUtils.sanitizeFilename(question);

            // Create the filename with .html extension for styled content
            String fileName = date + "_" + sanitized + ".html";

            // Create the content with Monokai-themed markdown
            String content = MarkdownRenderer.createMonokaiStyledMarkdown(question, response);

            // Write to file in questions directory
            Path questionsDir = Paths.get("questions");
            if (!Files.exists(questionsDir)) {
                Files.createDirectories(questionsDir);
            }

            Path filePath = questionsDir.resolve(fileName);
            Files.write(filePath, content.getBytes());

            System.out.println();
            System.out.println(filePath.toAbsolutePath());
            System.out.println();
        } catch (IOException e) {
            System.err.println("Error saving question to file: " + e.getMessage());
        }
    }


}