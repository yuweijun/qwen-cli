package com.example.askquery.util;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;

import java.util.Arrays;

/**
 * Utility class for rendering markdown content with HTML styling
 */
public class MarkdownRenderer {
    
    /**
     * Creates markdown content with Monokai color scheme styling
     */
    public static String createMonokaiStyledMarkdown(String question, String response) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <title>%s</title>
                    <style>
                        body {
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            background-color: #272822;
                            color: #f8f8f2;
                            margin: 0;
                            padding: 20px;
                            line-height: 1.6;
                        }
                        .container {
                            max-width: 800px;
                            margin: 0 auto;
                        }
                        h1 {
                            color: #a6e22e;
                            border-bottom: 2px solid #a6e22e;
                            padding-bottom: 10px;
                        }
                        h2 {
                            color: #66d9ef;
                        }
                        h3 {
                            color: #fd971f;
                        }
                        h4, h5, h6 {
                            color: #ae81ff;
                        }
                        code {
                            background-color: #3c3d38;
                            color: #fd971f;
                            padding: 2px 4px;
                            border-radius: 3px;
                            font-family: 'Consolas', 'Courier New', monospace;
                        }
                        pre {
                            background-color: #2d2d2d;
                            border-left: 4px solid #a6e22e;
                            padding: 15px;
                            overflow-x: auto;
                            border-radius: 4px;
                        }
                        pre code {
                            background-color: transparent;
                            color: inherit;
                            padding: 0;
                        }
                        blockquote {
                            border-left: 4px solid #75715e;
                            margin: 0;
                            padding: 0 15px;
                            color: #75715e;
                        }
                        ul, ol {
                            padding-left: 20px;
                        }
                        li {
                            margin: 5px 0;
                        }
                        a {
                            color: #66d9ef;
                            text-decoration: none;
                        }
                        a:hover {
                            text-decoration: underline;
                        }
                        table {
                            border-collapse: collapse;
                            width: 100%%;
                            margin: 15px 0;
                        }
                        th, td {
                            border: 1px solid #444;
                            padding: 8px 12px;
                            text-align: left;
                        }
                        th {
                            background-color: #3c3d38;
                            color: #a6e22e;
                        }
                        tr:nth-child(even) {
                            background-color: #2d2d2d;
                        }
                        .question {
                            background-color: #3c3d38;
                            padding: 15px;
                            border-radius: 5px;
                            margin-bottom: 20px;
                            border-left: 4px solid #a6e22e;
                        }
                        .response {
                            padding: 10px 0;
                        }
                        .timestamp {
                            color: #75715e;
                            font-size: 0.9em;
                            text-align: right;
                            margin-top: 20px;
                            font-style: italic;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="question">
                            <h1>❓ %s</h1>
                        </div>
                        
                        <div class="response">
                            %s
                        </div>
                        
                        <div class="timestamp">
                            Generated on %s
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                    escapeHtml(question),
                    escapeHtml(question),
                    convertMarkdownToHtml(response),
                    java.time.LocalDateTime.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                );
    }
    
    /**
     * Convert markdown to HTML using flexmark library with extensions
     */
    public static String convertMarkdownToHtml(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }
        
        // Configure flexmark with extensions
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, Arrays.asList(
                TablesExtension.create(),
                AutolinkExtension.create(),
                StrikethroughExtension.create(),
                TaskListExtension.create()
        ));
        
        // Configure parser options
        options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");
        options.set(TablesExtension.COLUMN_SPANS, false);
        options.set(TablesExtension.MIN_HEADER_ROWS, 1);
        options.set(TablesExtension.MAX_HEADER_ROWS, 1);
        options.set(TablesExtension.APPEND_MISSING_COLUMNS, true);
        options.set(TablesExtension.DISCARD_EXTRA_COLUMNS, true);
        options.set(TablesExtension.HEADER_SEPARATOR_COLUMN_MATCH, true);
        
        // Create parser and renderer
        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();
        
        // Parse and render
        return renderer.render(parser.parse(markdown));
    }
    
    /**
     * Escape HTML special characters
     */
    public static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#x27;");
    }
}