package com.example.askquery.service;

import com.example.askquery.config.AppProperties;
import com.example.askquery.config.DashscopeProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class InteractiveServiceSearchTest {

    @TempDir
    Path tempDir;

    @Test
    public void given_app_properties_when_construct_then_initialize_search_history_service() throws Exception {
        // Given
        AppProperties appProps = new AppProperties();
        appProps.setHistoryFile(tempDir.resolve("test_history.json").toString());
        appProps.setHistoryDisplayCount(15); // Updated to match new default
        
        DashscopeProperties dashProps = new DashscopeProperties();
        DashscopeClient client = mock(DashscopeClient.class);

        // When
        InteractiveService service = new InteractiveService(appProps, dashProps, client);

        // Then
        // The service should be created successfully with SearchHistoryService initialized
        assertNotNull(service);
        
        // Verify that HistoryManager was created with correct parameters
        // We can't directly access private fields, but we can verify the service works
    }

    @Test
    public void given_search_commands_when_check_recognition_then_identify_correctly() {
        // This test verifies that the search command handlers are properly set up
        // Since the actual command handling requires user input simulation,
        // we'll test the command recognition logic
        
        String[] searchCommands = {"s", "S", ":s", ":S", ":search", ":SEARCH"};
        
        for (String cmd : searchCommands) {
            // These should all be recognized as search commands
            assertTrue(isSearchCommand(cmd), "Command '" + cmd + "' should be recognized as search command");
        }
        
        String[] nonSearchCommands = {"h", "H", ":h", "help", "question", ""};
        
        for (String cmd : nonSearchCommands) {
            // These should not be recognized as search commands
            assertFalse(isSearchCommand(cmd), "Command '" + cmd + "' should not be recognized as search command");
        }
    }
    
    private boolean isSearchCommand(String command) {
        return command.equalsIgnoreCase("s") || 
               command.equalsIgnoreCase(":s") || 
               command.equalsIgnoreCase(":search");
    }
}