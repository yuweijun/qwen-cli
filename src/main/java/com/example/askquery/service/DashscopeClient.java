package com.example.askquery.service;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.example.askquery.config.DashscopeProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DashscopeClient {

    private final DashscopeProperties props;
    private final Generation generation;
    private final ObjectMapper mapper = new ObjectMapper();

    public DashscopeClient(DashscopeProperties props) {
        this.props = props;
        this.generation = new Generation();
    }

    /**
     * Send messages to the configured API endpoint using Dashscope SDK.
     * Converts the message format to Dashscope SDK Message objects.
     *
     * Returns the response body as a JsonNode for flexibility; caller extracts text.
     */
    public JsonNode sendMessages(List<Map<String, String>> messages, String model) {
        try {
            // Convert the message format to Dashscope Message objects
            List<Message> dashscopeMessages = messages.stream()
                    .map(msg -> Message.builder()
                            .role(msg.get("role"))
                            .content(msg.get("content"))
                            .build())
                    .collect(Collectors.toList());

            GenerationParam param = GenerationParam.builder()
                    .apiKey(props.getApi().getKey())
                    .model(model)
                    .messages(dashscopeMessages)
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .build();

            GenerationResult result = generation.call(param);

            // Convert the result to JsonNode for compatibility with existing code
            String resultJson = mapper.writeValueAsString(result);
            return mapper.readTree(resultJson);
        } catch (Exception ex) {
            // wrap into a JSON-like node with error info
            try {
                return mapper.createObjectNode().put("error", ex.getMessage());
            } catch (Exception e) {
                // If even creating the error node fails, return a basic object
                return mapper.createObjectNode();
            }
        }
    }

    /**
     * Helper to extract textual reply from the service response.
     * Extracts the content from the Dashscope SDK response format.
     * The response typically contains output.choices[0].message.content
     */
    public String extractText(JsonNode json) {
        if (json == null) return "";
        try {
            // Try the Dashscope SDK response format: output.choices[0].message.content
            JsonNode outputNode = json.path("output");
            if (!outputNode.isMissingNode()) {
                JsonNode choicesNode = outputNode.path("choices");
                if (choicesNode.isArray() && choicesNode.size() > 0) {
                    JsonNode messageNode = choicesNode.get(0).path("message");
                    if (!messageNode.isMissingNode()) {
                        JsonNode contentNode = messageNode.path("content");
                        if (contentNode.isTextual()) {
                            return contentNode.asText();
                        }
                    }
                }
            }

            // Check for error in response
            JsonNode errNode = json.path("err_msg");
            if (!errNode.isMissingNode() && errNode.isTextual()) {
                return "Error: " + errNode.asText();
            }

            // fallback: return whole JSON as pretty string
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (Exception ex) {
            return "Error extracting text: " + ex.getMessage();
        }
    }
}