package com.example.askquery.demo;

import com.example.askquery.util.BatRenderer;

/**
 * Demo utility to test bat integration for markdown rendering
 */
public class BatDemo {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Bat Integration Demo ===\n");
        
        // Check if bat is available
        System.out.println("Checking if bat is available...");
        boolean batAvailable = BatRenderer.isBatAvailable();
        System.out.println("Bat available: " + batAvailable);
        
        if (batAvailable) {
            // Show available themes
            System.out.println("\nAvailable themes:");
            String[] themes = BatRenderer.getAvailableThemes();
            for (String theme : themes) {
                System.out.println("  - " + theme);
            }
            
            // Test rendering with sample markdown
            String sampleMarkdown = """
                # Bat Rendering Test
                
                This is a **test** of bat markdown rendering capabilities.
                
                ## Features Demonstrated
                
                - Syntax highlighting
                - **Bold text**
                - *Italic text*
                - `Inline code`
                
                ## Code Example
                
                ```java
                public class HelloWorld {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!");
                    }
                }
                ```
                
                ## List Example
                
                1. First item
                2. Second item
                3. Third item
                
                > This is a blockquote that should be rendered nicely.
                
                [Visit GitHub](https://github.com) for more information.
                """;
            
            System.out.println("\n" + "=".repeat(50));
            System.out.println("Testing bat rendering with Monokai Extended theme:");
            System.out.println("=".repeat(50));
            
            boolean success = BatRenderer.renderToTerminal(sampleMarkdown, "Monokai Extended");
            System.out.println("\nRender successful: " + success);
            
            System.out.println("\n" + "=".repeat(50));
            System.out.println("Testing bat rendering with Dracula theme:");
            System.out.println("=".repeat(50));
            
            success = BatRenderer.renderToTerminal(sampleMarkdown, "Dracula");
            System.out.println("\nRender successful: " + success);
            
        } else {
            System.out.println("Bat is not available. Please install bat to use this feature.");
            System.out.println("Installation: brew install bat (on macOS)");
        }
        
        System.out.println("\n=== Demo Complete ===");
    }
}