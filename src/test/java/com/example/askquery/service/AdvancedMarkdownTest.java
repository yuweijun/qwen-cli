package com.example.askquery.service;

import com.example.askquery.config.AppProperties;
import com.example.askquery.config.DashscopeProperties;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AdvancedMarkdownTest {

    @Test
    public void testTableConversion() throws Exception {
        // Given
        AppProperties appProps = new AppProperties();
        appProps.setHistoryFile("/tmp/test_history.json");
        
        DashscopeProperties dashProps = new DashscopeProperties();
        DashscopeClient client = mock(DashscopeClient.class);
        
        InteractiveService service = new InteractiveService(appProps, dashProps, client);
        
        Method convertMethod = InteractiveService.class.getDeclaredMethod("convertMarkdownToHtml", String.class);
        convertMethod.setAccessible(true);
        
        // Test with a proper table
        String markdownTable = """
            | Header 1 | Header 2 |
            |----------|----------|
            | Cell 1   | Cell 2   |
            """;
        
        // When
        String html = (String) convertMethod.invoke(service, markdownTable);
        
        // Then - Tables should now work with flexmark
        System.out.println("Generated HTML for table test:");
        System.out.println(html);
        
        assertTrue(html.contains("<table>"));
        assertTrue(html.contains("<thead>"));
        assertTrue(html.contains("<tbody>"));
        assertTrue(html.contains("<th>Header 1</th>"));
        assertTrue(html.contains("<td>Cell 1</td>"));
    }

    @Test
    public void testAutolinkDetection() throws Exception {
        // Given
        AppProperties appProps = new AppProperties();
        appProps.setHistoryFile("/tmp/test_history.json");
        
        DashscopeProperties dashProps = new DashscopeProperties();
        DashscopeClient client = mock(DashscopeClient.class);
        
        InteractiveService service = new InteractiveService(appProps, dashProps, client);
        
        Method convertMethod = InteractiveService.class.getDeclaredMethod("convertMarkdownToHtml", String.class);
        convertMethod.setAccessible(true);
        
        String markdownWithUrls = "Visit https://example.com for more information.";
        
        // When
        String html = (String) convertMethod.invoke(service, markdownWithUrls);
        
        // Then
        assertTrue(html.contains("<a href=\"https://example.com\">https://example.com</a>"));
        System.out.println("Autolink HTML: " + html);
    }

    @Test
    public void testComplexMarkdown() throws Exception {
        // Given
        AppProperties appProps = new AppProperties();
        appProps.setHistoryFile("/tmp/test_history.json");
        
        DashscopeProperties dashProps = new DashscopeProperties();
        DashscopeClient client = mock(DashscopeClient.class);
        
        InteractiveService service = new InteractiveService(appProps, dashProps, client);
        
        Method convertMethod = InteractiveService.class.getDeclaredMethod("convertMarkdownToHtml", String.class);
        convertMethod.setAccessible(true);
        
        String complexMarkdown = """
            # Main Title
            
            ## Subsection
            
            This is **bold text** and *italic text* with `inline code`.
            
            [Link to Google](https://google.com)
            
            > This is a blockquote with multiple lines
            > that should be properly formatted.
            
            - List item 1
            - List item 2
              - Nested item
            
            1. Ordered item 1
            2. Ordered item 2
            
            ```javascript
            console.log('Hello World');
            ```
            """;
        
        // When
        String html = (String) convertMethod.invoke(service, complexMarkdown);
        
        // Then
        assertTrue(html.contains("<h1>Main Title</h1>"));
        assertTrue(html.contains("<strong>bold text</strong>"));
        assertTrue(html.contains("<em>italic text</em>"));
        assertTrue(html.contains("<code>inline code</code>"));
        assertTrue(html.contains("<a href=\"https://google.com\">Link to Google</a>"));
        assertTrue(html.contains("<blockquote>"));
        assertTrue(html.contains("<ul>"));
        assertTrue(html.contains("<ol>"));
        assertTrue(html.contains("<pre>"));
        System.out.println("Complex markdown HTML preview:");
        System.out.println(html.substring(0, Math.min(500, html.length())) + "...");
    }
}