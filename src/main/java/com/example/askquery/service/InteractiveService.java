package com.example.askquery.service;

import com.example.askquery.config.AppProperties;
import com.example.askquery.config.DashscopeProperties;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class InteractiveService {

    private final AppProperties appProps;
    private final DashscopeProperties dashProps;
    private final DashscopeClient client;

    private final Deque<Map<String, String>> convo = new ArrayDeque<>();
    private final List<Entry> entries = new ArrayList<>();
    private final ExecutorService executor;
    private final boolean parallelMode;

    public InteractiveService(AppProperties appProps, DashscopeProperties dashProps, DashscopeClient client) {
        this.appProps = appProps;
        this.dashProps = dashProps;
        this.client = client;
        this.parallelMode = appProps.isParallel();
        int threads = Math.max(1, appProps.getConcurrency());
        this.executor = parallelMode ? Executors.newFixedThreadPool(threads) : null;
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

    public void run(String initialQuery) throws Exception {
        String histPath = Objects.requireNonNullElse(appProps.getHistoryFile(),
                System.getProperty("user.home") + File.separator + ".ask_query_history");
        ensureHistoryFile(histPath);

        // Clear the history file to start fresh
        clearHistoryFile(histPath);

        Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .build();

        // Create a new history instance to avoid format issues
        DefaultHistory history = new DefaultHistory();

        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .parser(new DefaultParser())
                .variable(LineReader.HISTORY_FILE, Paths.get(histPath))
                .history(history)
                .build();

        // Attempt to load history, but handle errors gracefully
        try {
            history.attach(reader);
            history.load();
        } catch (Exception e) {
            // If history loading fails, continue without history
            System.err.println("Warning: Could not load history file: " + e.getMessage());
            System.err.println("History functionality may be limited.");
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
            appendHistoryUnique(histPath, initialQuery);
            submitAndMaybeWait(initialQuery, histPath, reader, exits);
        }

        String prompt = "请输入你的问题（或输入 exit/quit 退出；h 查看历史记录）： ";
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

            if (line == null) break;
            String s = line.trim();
            if (s.isEmpty()) continue;

            if (s.equalsIgnoreCase("h")) {
                showHistory(terminal);
                continue;
            }

            if (exits.contains(s.toLowerCase())) {
                System.out.println("收到退出命令，等待进行中的请求并退出...");
                break;
            }

            // Add to JLine history as well as file
            reader.getHistory().add(s);
            appendHistoryUnique(histPath, s);
            submitAndMaybeWait(s, histPath, reader, exits);
        }

        if (executor != null) {
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);
        }

        try {
            if (history instanceof DefaultHistory) {
                ((DefaultHistory) history).save();
            }
        } catch (Exception ignored) {
        }
    }


    private void submitAndMaybeWait(String query, String histPath, LineReader reader, List<String> exits) {
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
                }
            });
        }
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
        synchronized (entries) {
            if (entries.isEmpty()) {
                terminal.writer().println("\n没有历史记录。");
                terminal.flush();
                return;
            }

            terminal.writer().println("\n========== 历史记录 ==========");
            for (int i = 0; i < entries.size(); i++) {
                Entry entry = entries.get(i);
                terminal.writer().println("[" + (i + 1) + "] 问题: " + entry.question);
                if (entry.answer != null) {
                    terminal.writer().println("    回答: " + entry.answer);
                } else {
                    terminal.writer().println("    回答: (等待中...)");
                }
                terminal.writer().println("---------------------------");

                // Add five blank lines between questions (except for the last entry)
                if (i < entries.size() - 1) {
                    for (int j = 0; j < 5; j++) {
                        terminal.writer().println();
                    }
                }
            }
            terminal.writer().println("按 q 键或 Esc 键退出历史记录视图...");
            terminal.flush();
        }

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

        // Clear screen
        terminal.writer().println("\n".repeat(30));
        terminal.flush();
    }

    private void clearHistoryFile(String historyFile) {
        try {
            Path p = Paths.get(historyFile);
            if (Files.exists(p)) {
                // Clear the file by writing an empty string
                Files.write(p, new byte[0]);
            }
        } catch (IOException ignored) {
            // If we can't clear the file, continue anyway
        }
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
}