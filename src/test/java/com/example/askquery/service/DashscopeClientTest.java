package com.example.askquery.service;

import com.example.askquery.config.DashscopeProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DashscopeClientTest {

    @Mock
    private DashscopeProperties mockProps;

    private DashscopeClient dashscopeClient;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        dashscopeClient = new DashscopeClient(mockProps);
    }

    @Test
    public void testSendMessagesNotNull() {
        // Given
        List<Map<String, String>> messages = List.of(
            Map.of("role", "user", "content", "Hello")
        );
        String model = "qwen-plus";

        // When
        JsonNode result = dashscopeClient.sendMessages(messages, model);

        // Then
        assertNotNull(result);
    }

    @Test
    public void testExtractTextFromNull() {
        // Given
        JsonNode nullNode = null;

        // When
        String result = dashscopeClient.extractText(nullNode);

        // Then
        assertEquals("", result);
    }
}