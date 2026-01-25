package com.example.askquery;

import com.example.askquery.config.AppProperties;
import com.example.askquery.config.DashscopeProperties;
import com.example.askquery.service.DashscopeClient;
import com.example.askquery.service.InteractiveService;

public class Main {

    public static void main(String[] args) {
        try {
            // Load configuration
            AppProperties appProps = loadAppProperties();
            DashscopeProperties dashProps = loadDashscopeProperties();
            
            // Create service instances
            DashscopeClient client = new DashscopeClient(dashProps);
            InteractiveService interactiveService = new InteractiveService(appProps, dashProps, client);
            
            // If a first query is provided as program arg, pass it as initial query
            String initial = null;
            if (args != null && args.length > 0) {
                initial = args[0];
            }
            
            // Run the interactive service
            interactiveService.run(initial);
            
        } catch (Exception e) {
            System.err.println("Error running application: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static AppProperties loadAppProperties() {
        AppProperties props = new AppProperties();
        
        // Load from system properties or use defaults
        String historyFile = System.getProperty("app.historyFile", 
            System.getProperty("user.home") + ".qwen_cli_history");
        props.setHistoryFile(historyFile);
        
        String contextLengthStr = System.getProperty("app.contextLength", "6");
        try {
            props.setContextLength(Integer.parseInt(contextLengthStr));
        } catch (NumberFormatException e) {
            props.setContextLength(6);
        }
        
        String parallelStr = System.getProperty("app.parallel", "false");
        props.setParallel(Boolean.parseBoolean(parallelStr));
        
        String concurrencyStr = System.getProperty("app.concurrency", "2");
        try {
            props.setConcurrency(Integer.parseInt(concurrencyStr));
        } catch (NumberFormatException e) {
            props.setConcurrency(2);
        }
        
        String exitCommands = System.getProperty("app.exitCommands", "exit,quit,q");
        props.setExitCommands(exitCommands);
        
        String systemMessage = System.getProperty("app.systemMessage", "You are a helpful assistant.");
        props.setSystemMessage(systemMessage);
        
        return props;
    }

    private static DashscopeProperties loadDashscopeProperties() {
        DashscopeProperties props = new DashscopeProperties();
        
        // Load API key from system property or environment variable
        String apiKey = System.getProperty("dashscope.api.key");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKey = System.getenv("DASHSCOPE_API_KEY");
        }
        
        // Set the API key
        if (apiKey != null) {
            props.getApi().setKey(apiKey);
        }
        
        // Load model from system property or use default
        String model = System.getProperty("dashscope.model", "qwen-plus");
        props.setModel(model);
        
        return props;
    }
}