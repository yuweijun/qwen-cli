package com.example.askquery.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for rendering markdown content using bat command with syntax highlighting
 */
public class BatRenderer {

    private static String batCommand = "/usr/local/bin/bat";
    private static final String DEFAULT_THEME = "Monokai Extended";
    private static final int RENDER_TIMEOUT_SECONDS = 30;

    /**
     * Sets the bat command path
     */
    public static void setBatCommand(String command) {
        batCommand = command;
    }

    /**
     * Checks if bat command is available on the system
     */
    public static boolean isBatAvailable() {
        try {
            Process process = new ProcessBuilder(batCommand, "--version")
                    .start();
            return process.waitFor(RENDER_TIMEOUT_SECONDS, TimeUnit.SECONDS) &&
                    process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Renders markdown content to terminal using bat with syntax highlighting
     *
     * @param markdownContent The markdown content to render
     * @param theme           Optional theme name (defaults to Monokai Extended)
     * @return true if rendering was successful, false otherwise
     */
    public static boolean renderToTerminal(String markdownContent, String theme) {
        if (!isBatAvailable()) {
            return false;
        }

        String actualTheme = theme != null && !theme.isEmpty() ? theme : DEFAULT_THEME;

        try {
            // Create temporary file with markdown content
            Path tempFile = Files.createTempFile("qwen_response_", ".md");
            Files.write(tempFile, markdownContent.getBytes());

            try {
                // Build bat command with appropriate options
                ProcessBuilder pb = new ProcessBuilder(
                        batCommand,
                        tempFile.toString(),
                        "--language=md",
                        "--theme=" + actualTheme,
                        "--style=rule",
                        "--paging=never",
                        "--decorations=always",
                        "--color=always"
                );

                Process process = pb.start();

                // Capture and display output
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream())
                );

                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }

                int exitCode = process.waitFor(RENDER_TIMEOUT_SECONDS, TimeUnit.SECONDS) ? process.exitValue() : -1;

                return exitCode == 0;

            } finally {
                // Clean up temporary file
                Files.deleteIfExists(tempFile);
            }

        } catch (Exception e) {
            System.err.println("Error rendering with bat: " + e.getMessage());
            return false;
        }
    }

    /**
     * Renders markdown content to terminal using default theme
     *
     * @param markdownContent The markdown content to render
     * @return true if rendering was successful, false otherwise
     */
    public static boolean renderToTerminal(String markdownContent) {
        return renderToTerminal(markdownContent, null);
    }

    /**
     * Gets a list of available bat themes
     */
    public static String[] getAvailableThemes() {
        if (!isBatAvailable()) {
            return new String[0];
        }

        try {
            Process process = new ProcessBuilder(batCommand, "--list-themes").start();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            return reader.lines().toArray(String[]::new);

        } catch (Exception e) {
            return new String[0];
        }
    }

    /**
     * Validates if a theme is available
     */
    public static boolean isThemeAvailable(String theme) {
        if (theme == null || theme.isEmpty()) {
            return false;
        }

        String[] themes = getAvailableThemes();
        for (String availableTheme : themes) {
            if (availableTheme.trim().equals(theme)) {
                return true;
            }
        }
        return false;
    }
}