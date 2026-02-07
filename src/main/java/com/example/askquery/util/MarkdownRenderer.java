package com.example.askquery.util;

import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * Utility class for rendering markdown content
 */
public class MarkdownRenderer {
    
    private static final Parser parser;
    private static final HtmlRenderer renderer;
    
    static {
        MutableDataSet options = new MutableDataSet();
        // Enable table parsing and autolink by adding the extensions
        options.set(Parser.EXTENSIONS, Arrays.asList(
            TablesExtension.create(),
            AutolinkExtension.create()
        ));
        
        // Configure table options
        options.set(TablesExtension.COLUMN_SPANS, false)
               .set(TablesExtension.APPEND_MISSING_COLUMNS, true)
               .set(TablesExtension.DISCARD_EXTRA_COLUMNS, true)
               .set(TablesExtension.HEADER_SEPARATOR_COLUMN_MATCH, true);
        
        parser = Parser.builder(options).build();
        renderer = HtmlRenderer.builder(options).build();
    }
    
    /**
     * Escape HTML special characters in text
     */
    public static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#x27;");
    }
    
    /**
     * Convert markdown to HTML using flexmark library with extensions
     */
    public static String convertMarkdownToHtml(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }
        
        Node document = parser.parse(markdown);
        return renderer.render(document);
    }
    
    /**
     * Creates markdown content with Monokai color scheme styling
     */
    public static String createMonokaiStyledMarkdown(String question, String response) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"utf-8\">\n");
        html.append("    <title>").append(escapeHtml(question)).append("</title>\n");
        html.append("    <style>\n");
        html.append("        body {\n");
        html.append("            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n");
        html.append("            background-color: #272822;\n");
        html.append("            color: #f8f8f2;\n");
        html.append("            margin: 0;\n");
        html.append("            padding: 20px;\n");
        html.append("            line-height: 1.6;\n");
        html.append("        }\n");
        html.append("        .container {\n");
        html.append("            max-width: 800px;\n");
        html.append("            margin: 0 auto;\n");
        html.append("        }\n");
        html.append("        h1 {\n");
        html.append("            color: #a6e22e;\n");
        html.append("            border-bottom: 2px solid #a6e22e;\n");
        html.append("            padding-bottom: 10px;\n");
        html.append("        }\n");
        html.append("        h2 {\n");
        html.append("            color: #66d9ef;\n");
        html.append("        }\n");
        html.append("        h3 {\n");
        html.append("            color: #fd971f;\n");
        html.append("        }\n");
        html.append("        h4, h5, h6 {\n");
        html.append("            color: #ae81ff;\n");
        html.append("        }\n");
        html.append("        code {\n");
        html.append("            background-color: #3c3d38;\n");
        html.append("            color: #fd971f;\n");
        html.append("            padding: 2px 4px;\n");
        html.append("            border-radius: 3px;\n");
        html.append("            font-family: 'Consolas', 'Courier New', monospace;\n");
        html.append("        }\n");
        html.append("        pre {\n");
        html.append("            background-color: #2d2d2d;\n");
        html.append("            border-left: 4px solid #a6e22e;\n");
        html.append("            padding: 15px;\n");
        html.append("            overflow-x: auto;\n");
        html.append("            border-radius: 4px;\n");
        html.append("        }\n");
        html.append("        pre code {\n");
        html.append("            background-color: transparent;\n");
        html.append("            color: inherit;\n");
        html.append("            padding: 0;\n");
        html.append("        }\n");
        html.append("        blockquote {\n");
        html.append("            border-left: 4px solid #75715e;\n");
        html.append("            margin: 0;\n");
        html.append("            padding: 0 15px;\n");
        html.append("            color: #75715e;\n");
        html.append("        }\n");
        html.append("        ul, ol {\n");
        html.append("            padding-left: 20px;\n");
        html.append("        }\n");
        html.append("        li {\n");
        html.append("            margin: 5px 0;\n");
        html.append("        }\n");
        html.append("        a {\n");
        html.append("            color: #66d9ef;\n");
        html.append("            text-decoration: none;\n");
        html.append("        }\n");
        html.append("        a:hover {\n");
        html.append("            text-decoration: underline;\n");
        html.append("        }\n");
        html.append("        table {\n");
        html.append("            border-collapse: collapse;\n");
        html.append("            width: 100%;\n");
        html.append("            margin: 15px 0;\n");
        html.append("        }\n");
        html.append("        th, td {\n");
        html.append("            border: 1px solid #444;\n");
        html.append("            padding: 8px 12px;\n");
        html.append("            text-align: left;\n");
        html.append("        }\n");
        html.append("        th {\n");
        html.append("            background-color: #3c3d38;\n");
        html.append("            color: #a6e22e;\n");
        html.append("        }\n");
        html.append("        tr:nth-child(even) {\n");
        html.append("            background-color: #2d2d2d;\n");
        html.append("        }\n");
        html.append("        .question {\n");
        html.append("            background-color: #3c3d38;\n");
        html.append("            padding: 15px;\n");
        html.append("            border-radius: 5px;\n");
        html.append("            margin-bottom: 20px;\n");
        html.append("            border-left: 4px solid #a6e22e;\n");
        html.append("        }\n");
        html.append("        .response {\n");
        html.append("            padding: 10px 0;\n");
        html.append("        }\n");
        html.append("        .timestamp {\n");
        html.append("            color: #75715e;\n");
        html.append("            font-size: 0.9em;\n");
        html.append("            text-align: right;\n");
        html.append("            margin-top: 20px;\n");
        html.append("            font-style: italic;\n");
        html.append("        }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <div class=\"container\">\n");
        html.append("        <div class=\"question\">\n");
        html.append("            <h1>❓ ").append(escapeHtml(question)).append("</h1>\n");
        html.append("        </div>\n");
        html.append("        \n");
        html.append("        <div class=\"response\">\n");
        html.append("            ").append(convertMarkdownToHtml(response)).append("\n");
        html.append("        </div>\n");
        html.append("        \n");
        html.append("        <div class=\"timestamp\">\n");
        html.append("            Generated on ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        html.append("        </div>\n");
        html.append("    </div>\n");
        html.append("</body>\n");
        html.append("</html>\n");
        
        return html.toString();
    }
}