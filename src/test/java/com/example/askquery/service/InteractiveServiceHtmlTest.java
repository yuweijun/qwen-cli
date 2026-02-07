package com.example.askquery.service;

import com.example.askquery.config.AppProperties;
import com.example.askquery.config.DashscopeProperties;
import com.example.askquery.util.MarkdownRenderer;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class InteractiveServiceHtmlTest {

    @Test
    public void given_markdown_content_when_create_monokai_styled_html_then_contains_proper_styling() throws Exception {
        // Given
        String question = "What is Java?";
        String response = "**Java** is a programming language with ```code examples``` and *italic text*.";
        
        // When
        String htmlContent = MarkdownRenderer.createMonokaiStyledMarkdown(question, response);
        
        // Then
        assertNotNull(htmlContent);
        assertTrue(htmlContent.contains("<!DOCTYPE html>"));
        assertTrue(htmlContent.contains("background-color: #272822")); // Monokai background
        assertTrue(htmlContent.contains("color: #f8f8f2")); // Monokai text
        assertTrue(htmlContent.contains("color: #a6e22e")); // Monokai green for headers
        assertTrue(htmlContent.contains("<strong>Java</strong>")); // Bold conversion
        assertTrue(htmlContent.contains("<em>italic text</em>")); // Italic conversion
        assertTrue(htmlContent.contains("<code>code examples</code>")); // Code conversion
        assertTrue(htmlContent.contains(question)); // Contains the question
    }

    @Test
    public void given_dangerous_html_input_when_escape_html_then_return_escaped_output() throws Exception {
        // Given
        String dangerousInput = "<script>alert('xss')</script>";
        
        // When
        String escaped = MarkdownRenderer.escapeHtml(dangerousInput);
        
        // Then
        assertNotNull(escaped);
        assertFalse(escaped.contains("<script>"));
        assertTrue(escaped.contains("&lt;script&gt;"));
    }
}