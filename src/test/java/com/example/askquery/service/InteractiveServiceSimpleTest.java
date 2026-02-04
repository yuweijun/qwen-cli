package com.example.askquery.service;

import com.example.askquery.config.AppProperties;
import com.example.askquery.config.DashscopeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InteractiveService
 * Focuses on testing constructor behavior and configuration handling
 */
public class InteractiveServiceSimpleTest {

    private AppProperties appProps;
    private DashscopeProperties dashProps;
    private DashscopeClient client;

    @BeforeEach
    public void setUp() {
        // Create real configuration objects for testing
        appProps = createValidAppProperties();
        dashProps = createValidDashscopeProperties();
        client = mock(DashscopeClient.class);
    }

    @Test
    public void testConstructorCreatesServiceSuccessfully() {
        // When
        InteractiveService service = new InteractiveService(appProps, dashProps, client);

        // Then
        assertNotNull(service, "InteractiveService should be created successfully");
    }

    @Test
    public void testConstructorHandlesNullHistoryFile() {
        // Given
        appProps.setHistoryFile(null);

        // When & Then - Currently throws NPE, this documents the expected behavior
        assertThrows(NullPointerException.class, () -> {
            InteractiveService service = new InteractiveService(appProps, dashProps, client);
        }, "Service should throw NPE when history file is null (current behavior)");
    }

    @Test
    public void testConstructorWithParallelMode() {
        // Given
        appProps.setParallel(true);
        appProps.setConcurrency(4);

        // When
        InteractiveService service = new InteractiveService(appProps, dashProps, client);

        // Then
        assertNotNull(service, "Service should work in parallel mode");
    }

    @Test
    public void testConstructorWithZeroContextLength() {
        // Given
        appProps.setContextLength(0);

        // When
        InteractiveService service = new InteractiveService(appProps, dashProps, client);

        // Then
        assertNotNull(service, "Service should handle zero context length");
    }

    @Test
    public void testConstructorWithNegativeContextLength() {
        // Given
        appProps.setContextLength(-1);

        // When & Then - Should handle gracefully
        assertDoesNotThrow(() -> {
            InteractiveService service = new InteractiveService(appProps, dashProps, client);
            assertNotNull(service, "Service should handle negative context length gracefully");
        });
    }

    @Test
    public void testConstructorWithEmptyExitCommands() {
        // Given
        appProps.setExitCommands("");

        // When
        InteractiveService service = new InteractiveService(appProps, dashProps, client);

        // Then
        assertNotNull(service, "Service should handle empty exit commands");
    }

    @Test
    public void testConstructorWithNullSystemMessage() {
        // Given
        appProps.setSystemMessage(null);

        // When & Then - Should handle gracefully
        assertDoesNotThrow(() -> {
            InteractiveService service = new InteractiveService(appProps, dashProps, client);
            assertNotNull(service, "Service should handle null system message");
        });
    }

    @Test
    public void testMultipleServicesCanBeCreated() {
        // When
        InteractiveService service1 = new InteractiveService(appProps, dashProps, client);
        InteractiveService service2 = new InteractiveService(appProps, dashProps, client);

        // Then
        assertNotNull(service1);
        assertNotNull(service2);
        assertNotSame(service1, service2, "Should create separate instances");
    }

    @Test
    public void testServiceCreationWithDifferentConfigurations() {
        // Given different configurations
        AppProperties props1 = createValidAppProperties();
        props1.setContextLength(3);
        props1.setParallel(false);
        
        AppProperties props2 = createValidAppProperties();
        props2.setContextLength(10);
        props2.setParallel(true);

        // When & Then - Should handle gracefully
        assertDoesNotThrow(() -> {
            InteractiveService service1 = new InteractiveService(props1, dashProps, client);
            InteractiveService service2 = new InteractiveService(props2, dashProps, client);
            
            assertNotNull(service1);
            assertNotNull(service2);
            assertNotSame(service1, service2, "Should handle different configurations");
        });
    }

    // Helper methods to create valid configurations
    private AppProperties createValidAppProperties() {
        AppProperties props = new AppProperties();
        props.setHistoryFile("/tmp/test_history.json");
        props.setContextLength(6);
        props.setParallel(false);
        props.setConcurrency(2);
        props.setExitCommands("exit,quit,q");
        props.setSystemMessage("You are a helpful assistant.");
        return props;
    }

    private DashscopeProperties createValidDashscopeProperties() {
        DashscopeProperties props = new DashscopeProperties();
        props.setModel("qwen-plus");
        return props;
    }
}