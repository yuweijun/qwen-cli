package com.example.askquery.service;

import com.example.askquery.config.AppProperties;
import com.example.askquery.config.DashscopeProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.springframework.stereotype.Service;

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

@Service
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

        Terminal terminal = TerminalBuilder.builder().system(true).build();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .parser(new DefaultParser())
                .variable(LineReader.HISTORY_FILE, Paths.get(histPath))
                .build();

        History history = reader.getHistory();
        if (history instanceof DefaultHistory) {
            ((DefaultHistory) history).attach(reader);
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

        String prompt = "请输入你的问题（或输入 exit/quit 退出；:nav/:view/navi 进入浏览模式）： ";
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

            if (s.equalsIgnoreCase(":nav") || s.equalsIgnoreCase(":view") || s.equalsIgnoreCase("navi")) {
                enterNavigationMode(reader, terminal);
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

    private void enterNavigationMode(LineReader reader, Terminal terminal) {
        synchronized (entries) {
            if (entries.isEmpty()) {
                terminal.writer().println("\n没有可浏览的记录（还没有问答完成）。按回车继续。");
                terminal.flush();
                try {
                    reader.readLine();
                } catch (Exception ignored) {
                }
                return;
            }
        }

        int idx;
        synchronized (entries) {
            idx = entries.size() - 1;
        }

        terminal.writer().println("\n进入浏览模式：使用 ← / → / ↑ / ↓ (Left/Right/Up/Down) 浏览历史记录，按 Enter 或 q 退出浏览模式。");
        terminal.flush();

        boolean running = true;
        while (running) {
            showEntryAt(terminal, idx);

            int ch;
            try {
                ch = terminal.reader().read();
            } catch (IOException | IllegalStateException e) {
                break;
            }

            if (ch == 13 || ch == 10) {  // Enter key
                running = false;
                break;
            } else if (ch == 'q' || ch == 'Q') {  // q key to quit
                running = false;
                break;
            } else if (ch == 27) {  // ESC character, start of arrow key sequence
                try {
                    int c2 = terminal.reader().read();
                    if (c2 == 91) {  // '[' character
                        int c3 = terminal.reader().read();
                        synchronized (entries) {
                            if (c3 == 68) {  // Left arrow key
                                if (idx > 0) idx--;
                            } else if (c3 == 67) {  // Right arrow key
                                if (idx < entries.size() - 1) idx++;
                            } else if (c3 == 65) {  // Up arrow key
                                if (idx > 0) idx--;
                            } else if (c3 == 66) {  // Down arrow key
                                if (idx < entries.size() - 1) idx++;
                            }
                        }
                    }
                } catch (IOException ignored) {
                }
            } else if (ch == 3) {  // Ctrl+C to force quit
                running = false;
                break;
            }
        }

        terminal.writer().println("退出浏览模式，返回提示。");
        terminal.flush();
    }

    private void showEntryAt(Terminal terminal, int idx) {
        terminal.writer().println("\n------------------------------");
        int totalEntries;
        synchronized (entries) {
            totalEntries = entries.size();
        }
        Entry e;
        synchronized (entries) {
            if (idx < 0 || idx >= entries.size()) {
                terminal.writer().println("[空白索引]");
                terminal.flush();
                return;
            }
            e = entries.get(idx);
        }
        terminal.writer().println("条目 " + (idx + 1) + "/" + totalEntries + " | 索引: " + idx + "  问题: " + e.question);
        terminal.writer().println("答案: ");
        if (e.answer == null) {
            terminal.writer().println("  (尚未返回结果，可能正在处理中)");
        } else {
            String[] lines = e.answer.split("\\r?\\n");
            for (String line : lines) {
                terminal.writer().println("  " + line);
            }
        }
        terminal.writer().println("------------------------------");
        terminal.flush();
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