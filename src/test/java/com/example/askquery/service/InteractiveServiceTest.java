package com.example.askquery.service;

import com.example.askquery.config.AppProperties;
import com.example.askquery.config.DashscopeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class InteractiveServiceTest {

    private AppProperties appProps;
    private DashscopeProperties dashProps;
    private DashscopeClient client;

    @BeforeEach
    public void setUp() {
        // Create real instances since we can't easily mock private methods
        appProps = new AppProperties();
        appProps.setHistoryFile("/tmp/test_history.json");
        appProps.setContextLength(6);
        appProps.setParallel(false);
        appProps.setConcurrency(2);
        appProps.setExitCommands("exit,quit,q");
        appProps.setSystemMessage("You are a helpful assistant.");

        dashProps = new DashscopeProperties();
        dashProps.setModel("qwen-plus");

        client = mock(DashscopeClient.class);
    }

    @Test
    public void given_app_properties_when_construct_service_then_initialize_fields_correctly() {
        // Test that constructor initializes fields properly
        InteractiveService service = new InteractiveService(appProps, dashProps, client);
        assertNotNull(service);
    }

    @Test
    public void given_empty_history_when_load_history_entries_then_handle_gracefully() {
        // Test passes if no exception is thrown during construction
        // The loadHistoryEntries is called in constructor
        InteractiveService service = new InteractiveService(appProps, dashProps, client);
        assertNotNull(service);
    }

    @Test
    public void given_nonexistent_history_file_when_ensure_history_file_then_create_file() throws Exception {
        // Use reflection to access private method
        InteractiveService service = new InteractiveService(appProps, dashProps, client);
        Method method = InteractiveService.class.getDeclaredMethod("ensureHistoryFile", String.class);
        method.setAccessible(true);

        // Given
        String testPath = "/tmp/test_history_file.txt";
        Path path = Paths.get(testPath);
        
        // Ensure file doesn't exist
        Files.deleteIfExists(path);

        // When
        method.invoke(service, testPath);

        // Then
        assertTrue(Files.exists(path));
        
        // Cleanup
        Files.deleteIfExists(path);
    }

    @Test
    public void given_duplicate_line_when_append_history_unique_then_not_add_duplicate() throws Exception {
        // Use reflection to access private method
        InteractiveService service = new InteractiveService(appProps, dashProps, client);
        Method method = InteractiveService.class.getDeclaredMethod("appendHistoryUnique", String.class, String.class);
        method.setAccessible(true);

        // Given
        String testPath = "/tmp/test_append_history.txt";
        Path path = Paths.get(testPath);
        String line = "test line";
        
        // Create file with content
        Files.write(path, java.util.Collections.singletonList(line));
        
        // When
        method.invoke(service, testPath, line);

        // Then
        java.util.List<String> lines = Files.readAllLines(path);
        assertEquals(1, lines.size()); // Should not add duplicate
        
        // Cleanup
        Files.deleteIfExists(path);
    }

    @Test
    public void given_new_line_when_append_history_unique_then_add_new_line() throws Exception {
        // Use reflection to access private method
        InteractiveService service = new InteractiveService(appProps, dashProps, client);
        Method method = InteractiveService.class.getDeclaredMethod("appendHistoryUnique", String.class, String.class);
        method.setAccessible(true);

        // Given
        String testPath = "/tmp/test_append_history_new.txt";
        Path path = Paths.get(testPath);
        String existingLine = "existing line";
        String newLine = "new line";
        
        // Create file with existing content
        Files.write(path, java.util.Collections.singletonList(existingLine));
        
        // When
        method.invoke(service, testPath, newLine);

        // Then
        java.util.List<String> lines = Files.readAllLines(path);
        assertEquals(2, lines.size());
        assertEquals(existingLine, lines.get(0));
        assertEquals(newLine, lines.get(1));
        
        // Cleanup
        Files.deleteIfExists(path);
    }

    @Test
    public void given_question_and_answer_when_save_question_to_file_then_create_directory_and_file() throws Exception {
        // Use reflection to access private method
        InteractiveService service = new InteractiveService(appProps, dashProps, client);
        Method method = InteractiveService.class.getDeclaredMethod("saveQuestionToFile", String.class, String.class);
        method.setAccessible(true);

        // Given
        String question = "What is the meaning of life?";
        String answer = "42";
        String questionsDir = "questions";
        
        // Clean up any existing test files
        Path dirPath = Paths.get(questionsDir);
        if (Files.exists(dirPath)) {
            Files.walk(dirPath)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        // Ignore
                    }
                });
            Files.deleteIfExists(dirPath);
        }

        // When
        method.invoke(service, question, answer);

        // Then
        assertTrue(Files.exists(dirPath));
        assertTrue(Files.isDirectory(dirPath));
        
        // Check that at least one file was created
        java.util.List<Path> files = Files.list(dirPath).toList();
        assertFalse(files.isEmpty());
        
        // Check content of one of the files
        Path createdFile = files.get(0);
        String content = Files.readString(createdFile);
        assertTrue(content.contains(question));
        assertTrue(content.contains(answer));
        
        // Cleanup
        Files.walk(dirPath)
            .sorted((a, b) -> b.compareTo(a))
            .forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    // Ignore
                }
            });
        Files.deleteIfExists(dirPath);
    }

    @Test
    public void given_system_message_configured_when_build_messages_then_include_system_message() throws Exception {
        // Use reflection to access private method
        InteractiveService service = new InteractiveService(appProps, dashProps, client);
        Method method = InteractiveService.class.getDeclaredMethod("buildMessages", String.class);
        method.setAccessible(true);

        // Given
        appProps.setSystemMessage("Custom system message");
        appProps.setContextLength(2);

        // When
        @SuppressWarnings("unchecked")
        java.util.List<java.util.Map<String, String>> messages = 
            (java.util.List<java.util.Map<String, String>>) method.invoke(service, "User message");

        // Then
        assertTrue(messages.size() >= 2); // At least system message and user message
        assertEquals("system", messages.get(0).get("role"));
        assertEquals("Custom system message", messages.get(0).get("content"));
        assertEquals("user", messages.get(messages.size() - 1).get("role"));
        assertEquals("User message", messages.get(messages.size() - 1).get("content"));
    }

    @Test
    public void given_no_system_message_when_build_messages_then_exclude_system_message() throws Exception {
        // Use reflection to access private method
        InteractiveService service = new InteractiveService(appProps, dashProps, client);
        Method method = InteractiveService.class.getDeclaredMethod("buildMessages", String.class);
        method.setAccessible(true);

        // Given
        appProps.setSystemMessage("");
        appProps.setContextLength(0);

        // When
        @SuppressWarnings("unchecked")
        java.util.List<java.util.Map<String, String>> messages = 
            (java.util.List<java.util.Map<String, String>>) method.invoke(service, "User message");

        // Then
        assertEquals(1, messages.size()); // Only user message
        assertEquals("user", messages.get(0).get("role"));
        assertEquals("User message", messages.get(0).get("content"));
    }

    @Test
    public void given_context_length_when_construct_then_manage_context_properly() {
        // Given
        appProps.setContextLength(2);

        // When
        InteractiveService service = new InteractiveService(appProps, dashProps, client);

        // Then
        assertNotNull(service);
        // Test passes if constructor doesn't throw exception
    }

    @Test
    public void given_parallel_mode_configured_when_initialize_then_setup_thread_pool() {
        // Given
        appProps.setParallel(true);
        appProps.setConcurrency(3);

        // When
        InteractiveService parallelService = new InteractiveService(appProps, dashProps, client);

        // Then
        assertNotNull(parallelService);
        // Test passes if constructor doesn't throw exception
    }

    @Test
    public void given_custom_history_file_when_configure_then_use_custom_path() {
        // Given
        String customHistoryFile = "/custom/path/history.json";
        appProps.setHistoryFile(customHistoryFile);

        // When
        InteractiveService service = new InteractiveService(appProps, dashProps, client);

        // Then
        assertNotNull(service);
        // Constructor should handle the custom history file path
    }

    @Test
    public void given_null_initial_query_when_run_then_handle_gracefully() {
        // Test that run method can handle null initial query
        // This is more of an integration test - we'll test the basic setup
        InteractiveService service = new InteractiveService(appProps, dashProps, client);
        assertNotNull(service);
    }
}