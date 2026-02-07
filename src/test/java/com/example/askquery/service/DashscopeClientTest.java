package com.example.askquery.service;

import com.example.askquery.config.DashscopeProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

public class DashscopeClientTest {

    @Test
    public void given_valid_messages_when_send_messages_then_return_non_null_result() {
        // Given
        DashscopeProperties props = new DashscopeProperties();
        props.getApi().setKey(System.getenv("DASHSCOPE_API_KEY")); // Use actual properties
        DashscopeClient dashscopeClient = new DashscopeClient(props);

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
    public void given_null_json_node_when_extract_text_then_return_empty_string() {
        // Given
        DashscopeProperties props = new DashscopeProperties();
        props.getApi().setKey(System.getenv("DASHSCOPE_API_KEY")); // Use actual properties
        DashscopeClient dashscopeClient = new DashscopeClient(props);

        JsonNode nullNode = null;

        // When
        String result = dashscopeClient.extractText(nullNode);

        // Then
        assertEquals("", result);
    }
}