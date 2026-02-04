package com.example.askquery.service;

import com.example.askquery.config.AppProperties;
import com.example.askquery.config.DashscopeProperties;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class InteractiveServiceHtmlTest {

    @Test
    public void testHtmlGenerationContainsMonokaiStyling() throws Exception {
        // Given
        AppProperties appProps = new AppProperties();
        appProps.setHistoryFile("/tmp/test_history.json");
        
        DashscopeProperties dashProps = new DashscopeProperties();
        DashscopeClient client = mock(DashscopeClient.class);
        
        InteractiveService service = new InteractiveService(appProps, dashProps, client);
        
        // Use reflection to test the HTML generation method
        Method createMethod = InteractiveService.class.getDeclaredMethod("createMonokaiStyledMarkdown", String.class, String.class);
        createMethod.setAccessible(true);
        
        String question = "What is Java?";
        String response = "**Java** is a programming language with ```code examples``` and *italic text*.";
        
        // When
        String htmlContent = (String) createMethod.invoke(service, question, response);
        
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
    public void testEscapeHtmlMethod() throws Exception {
        // Given
        AppProperties appProps = new AppProperties();
        appProps.setHistoryFile("/tmp/test_history.json");
        
        DashscopeProperties dashProps = new DashscopeProperties();
        DashscopeClient client = mock(DashscopeClient.class);
        
        InteractiveService service = new InteractiveService(appProps, dashProps, client);
        
        // Use reflection to test the escape method
        Method escapeMethod = InteractiveService.class.getDeclaredMethod("escapeHtml", String.class);
        escapeMethod.setAccessible(true);
        
        String dangerousInput = "<script>alert('xss')</script>";
        
        // When
        String escaped = (String) escapeMethod.invoke(service, dangerousInput);
        
        // Then
        assertNotNull(escaped);
        assertFalse(escaped.contains("<script>"));
        assertTrue(escaped.contains("&lt;script&gt;"));
    }
}