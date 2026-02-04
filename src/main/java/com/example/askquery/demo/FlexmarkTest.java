package com.example.askquery.demo;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.ext.tables.TablesExtension;

import java.util.Arrays;

public class FlexmarkTest {
    public static void main(String[] args) {
        // Configure flexmark
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create()));
        
        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();
        
        // Test table parsing
        String markdownTable = """
            | Name | Age | City |
            |------|-----|------|
            | John | 25  | NYC  |
            | Jane | 30  | LA   |
            """;
        
        String html = renderer.render(parser.parse(markdownTable));
        
        System.out.println("Input:");
        System.out.println(markdownTable);
        System.out.println("\nOutput:");
        System.out.println(html);
        System.out.println("\nContains table: " + html.contains("<table>"));
    }
}