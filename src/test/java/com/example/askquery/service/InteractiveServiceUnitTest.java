package com.example.askquery.service;

import com.example.askquery.config.AppProperties;
import com.example.askquery.config.DashscopeProperties;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic unit tests for InteractiveService
 * Tests constructor and basic configuration handling
 */
public class InteractiveServiceUnitTest {

    @Test
    public void given_valid_configuration_when_construct_then_create_service_successfully() {
        // Given
        AppProperties appProps = createValidAppProperties();
        DashscopeProperties dashProps = createValidDashscopeProperties();
        DashscopeClient client = new DashscopeClient(dashProps);

        // When
        InteractiveService service = new InteractiveService(appProps, dashProps, client);

        // Then
        assertNotNull(service);
    }

    @Test
    public void given_minimal_configuration_when_construct_then_handle_gracefully() {
        // Given - minimal valid configuration
        AppProperties appProps = new AppProperties();
        appProps.setHistoryFile("/tmp/minimal_test_history.json");
        
        DashscopeProperties dashProps = new DashscopeProperties();
        DashscopeClient client = new DashscopeClient(dashProps);

        // When
        InteractiveService service = new InteractiveService(appProps, dashProps, client);

        // Then
        assertNotNull(service);
    }

    @Test
    public void given_different_context_lengths_when_create_services_then_handle_appropriately() {
        // Given
        AppProperties appProps = createValidAppProperties();
        DashscopeProperties dashProps = createValidDashscopeProperties();
        DashscopeClient client = new DashscopeClient(dashProps);

        // Test various context lengths
        int[] lengths = {0, 1, 5, 10};
        
        for (int length : lengths) {
            appProps.setContextLength(length);
            
            // When
            InteractiveService service = new InteractiveService(appProps, dashProps, client);
            
            // Then
            assertNotNull(service, "Should handle context length: " + length);
        }
    }

    @Test
    public void given_parallel_mode_configured_when_construct_then_setup_correctly() {
        // Given
        AppProperties appProps = createValidAppProperties();
        appProps.setParallel(true);
        appProps.setConcurrency(3);
        
        DashscopeProperties dashProps = createValidDashscopeProperties();
        DashscopeClient client = new DashscopeClient(dashProps);

        // When
        InteractiveService service = new InteractiveService(appProps, dashProps, client);

        // Then
        assertNotNull(service);
    }

    @Test
    public void given_multiple_constructions_when_create_services_then_create_separate_instances() {
        // Given
        AppProperties appProps = createValidAppProperties();
        DashscopeProperties dashProps = createValidDashscopeProperties();
        DashscopeClient client = new DashscopeClient(dashProps);

        // When
        InteractiveService service1 = new InteractiveService(appProps, dashProps, client);
        InteractiveService service2 = new InteractiveService(appProps, dashProps, client);

        // Then
        assertNotNull(service1);
        assertNotNull(service2);
        assertNotSame(service1, service2);
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