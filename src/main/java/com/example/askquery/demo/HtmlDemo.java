package com.example.askquery.demo;

import com.example.askquery.config.AppProperties;
import com.example.askquery.config.DashscopeProperties;
import com.example.askquery.service.InteractiveService;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Demo utility to show the Monokai-styled HTML generation with advanced markdown features
 */
public class HtmlDemo {
    public static void main(String[] args) throws Exception {
        // Create service instance
        AppProperties appProps = new AppProperties();
        appProps.setHistoryFile("/tmp/demo_history.json");
        
        DashscopeProperties dashProps = new DashscopeProperties();
        InteractiveService service = new InteractiveService(appProps, dashProps, null);
        
        // Sample content with working markdown features
        String question = "What are the key features of modern web development?";
        String response = """
            Modern web development encompasses many technologies and practices:
            
            ## Core Technologies
            
            **Frontend Frameworks:**
            - React, Vue.js, Angular
            - Svelte, SolidJS
            
            **Backend Technologies:**
            - Node.js with Express
            - Python with Django/FastAPI
            - Java with Spring Boot
            
            ## Development Tools Comparison
            
            ### Package Managers Comparison Table
            | Tool | Description | Popularity | Learning Curve |
            |------|-------------|------------|----------------|
            | npm | Node Package Manager | ⭐⭐⭐⭐⭐ | Easy |
            | yarn | Facebook's alternative | ⭐⭐⭐⭐ | Medium |
            | pnpm | Performance-focused | ⭐⭐⭐ | Hard |
            
            ### Build Tools Feature Matrix
            | Tool | Speed | Bundle Size | HMR Support | Configuration |
            |------|-------|-------------|-------------|---------------|
            | Webpack | Medium | Large | Yes | Complex |
            | Vite | Fast | Small | Yes | Simple |
            | Rollup | Fast | Small | Limited | Moderate |
            
            ### Framework Performance Benchmarks
            | Framework | Render Time (ms) | Bundle Size (KB) | Stars |
            |-----------|------------------|------------------|-------|
            | React | 15.2 | 42 | 205K |
            | Vue.js | 12.8 | 33 | 201K |
            | Svelte | 8.5 | 22 | 67K |
            
            ## Best Practices
            
            1. **Responsive Design**
               - Mobile-first approach
               - Flexible grid layouts
               - Media queries for breakpoints
            
            2. **Performance Optimization**
               - Image optimization techniques
               - Code splitting strategies
               - Lazy loading implementation
               
            3. **Security Measures**
               - Input validation and sanitization
               - Authentication and authorization
               - Secure HTTP headers
            
            ## Useful Resources
            
            Check out these helpful resources:
            
            - [MDN Web Docs](https://developer.mozilla.org) - Comprehensive web documentation
            - [GitHub](https://github.com) - Code hosting and collaboration
            - [Stack Overflow](https://stackoverflow.com) - Developer community Q&A
            
            Visit https://web.dev for modern web development guides.
            
            > "The best way to predict the future is to invent it." - Alan Kay
            
            For more information, visit [web.dev](https://web.dev) or email us at contact@example.com.
            """;
        
        // Use reflection to call the private method
        Method createMethod = InteractiveService.class.getDeclaredMethod("createMonokaiStyledMarkdown", String.class, String.class);
        createMethod.setAccessible(true);
        
        String htmlContent = (String) createMethod.invoke(service, question, response);
        
        // Write to file
        Path outputPath = Path.of("demo/modern_web_dev_demo.html");
        Files.write(outputPath, htmlContent.getBytes());
        
        System.out.println("Modern web development demo HTML generated at: " + outputPath.toAbsolutePath());
        System.out.println("Open this file in your browser to see:");
        System.out.println("- Properly formatted headers and lists");
        System.out.println("- Bold and italic text");
        System.out.println("- Links (manual and automatic)");
        System.out.println("- Blockquotes");
        System.out.println("- Tables with proper formatting");
        System.out.println("- All standard markdown elements");
        System.out.println("- Beautiful Monokai styling!");
    }
}