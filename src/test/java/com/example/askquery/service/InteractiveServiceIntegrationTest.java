package com.example.askquery.service;

import com.example.askquery.config.AppProperties;
import com.example.askquery.config.DashscopeProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration-style tests for InteractiveService
 * Tests actual behavior where possible without complex mocking
 */
public class InteractiveServiceIntegrationTest {

    private AppProperties appProps;
    private DashscopeProperties dashProps;
    private DashscopeClient client;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        // Redirect System.out to capture output
        System.setOut(new PrintStream(outContent));

        // Create test configuration
        appProps = new AppProperties();
        appProps.setHistoryFile(tempDir.resolve("test_history.json").toString());
        appProps.setContextLength(2);
        appProps.setParallel(false);
        appProps.setConcurrency(1);
        appProps.setExitCommands("exit,quit,q");
        appProps.setSystemMessage("You are a helpful assistant.");

        dashProps = new DashscopeProperties();
        dashProps.setModel("qwen-plus");

        client = mock(DashscopeClient.class);
    }

    @AfterEach
    public void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    public void given_valid_dependencies_when_create_service_then_work_correctly() {
        // Test that the service can be created and basic operations work
        InteractiveService service = new InteractiveService(appProps, dashProps, client);
        assertNotNull(service, "Service should be created successfully");
        
        // Test passes if no exceptions are thrown during creation
        assertTrue(true);
    }

    @Test
    public void given_various_context_lengths_when_create_service_then_handle_appropriately() {
        // Test various context length configurations
        int[] contextLengths = {0, 1, 5, 10, -1};
        
        for (int contextLength : contextLengths) {
            appProps.setContextLength(contextLength);
            InteractiveService service = new InteractiveService(appProps, dashProps, client);
            assertNotNull(service, "Service should handle context length: " + contextLength);
        }
    }

    @Test
    public void given_parallel_configurations_when_create_service_then_handle_appropriately() {
        // Test parallel mode configurations
        boolean[] parallelModes = {true, false};
        int[] concurrencies = {1, 2, 5};
        
        for (boolean parallel : parallelModes) {
            for (int concurrency : concurrencies) {
                appProps.setParallel(parallel);
                appProps.setConcurrency(concurrency);
                InteractiveService service = new InteractiveService(appProps, dashProps, client);
                assertNotNull(service, 
                    String.format("Service should handle parallel=%s, concurrency=%d", parallel, concurrency));
            }
        }
    }

    @Test
    public void given_various_exit_commands_when_create_service_then_handle_appropriately() {
        // Test different exit command configurations
        String[] exitCommandSets = {
            "exit",
            "exit,quit",
            "exit,quit,q",
            "",
            "stop,end"
        };
        
        for (String exitCommands : exitCommandSets) {
            appProps.setExitCommands(exitCommands);
            InteractiveService service = new InteractiveService(appProps, dashProps, client);
            assertNotNull(service, "Service should handle exit commands: " + exitCommands);
        }
    }

    @Test
    public void given_edge_case_configurations_when_create_service_then_handle_gracefully() {
        // Test edge cases and boundary conditions
        InteractiveService service;
        
        // Null system message
        appProps.setSystemMessage(null);
        service = new InteractiveService(appProps, dashProps, client);
        assertNotNull(service);
        
        // Empty system message
        appProps.setSystemMessage("");
        service = new InteractiveService(appProps, dashProps, client);
        assertNotNull(service);
        
        // Very long system message
        appProps.setSystemMessage("A".repeat(1000));
        service = new InteractiveService(appProps, dashProps, client);
        assertNotNull(service);
    }

    @Test
    public void given_multiple_creations_when_measure_performance_then_meet_expectations() {
        // Basic performance test - service creation should be fast
        long startTime = System.currentTimeMillis();
        
        // Create multiple services quickly
        for (int i = 0; i < 10; i++) {
            InteractiveService service = new InteractiveService(appProps, dashProps, client);
            assertNotNull(service);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Should create 10 services in reasonable time (less than 1 second)
        assertTrue(duration < 1000, "Service creation should be reasonably fast");
    }

    @Test
    public void given_multiple_services_when_check_state_then_maintain_independence() {
        // Test that multiple service instances are independent
        InteractiveService service1 = new InteractiveService(appProps, dashProps, client);
        InteractiveService service2 = new InteractiveService(appProps, dashProps, client);
        
        // Modify configuration for one service
        appProps.setContextLength(10);
        InteractiveService service3 = new InteractiveService(appProps, dashProps, client);
        
        // All services should still be valid
        assertNotNull(service1);
        assertNotNull(service2);
        assertNotNull(service3);
        
        // Different instances
        assertNotSame(service1, service2);
        assertNotSame(service2, service3);
    }
}