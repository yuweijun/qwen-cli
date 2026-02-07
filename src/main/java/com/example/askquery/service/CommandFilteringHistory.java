package com.example.askquery.service;

import org.jline.reader.History;
import org.jline.reader.impl.history.DefaultHistory;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Custom history implementation that filters out command inputs
 */
public class CommandFilteringHistory extends DefaultHistory {
    
    private final InteractiveService interactiveService;
    
    public CommandFilteringHistory(InteractiveService interactiveService) {
        this.interactiveService = interactiveService;
    }
    
    @Override
    public void add(String line) {
        // Only add to history if it's not a command
        if (interactiveService != null && !interactiveService.isCommand(line)) {
            super.add(line);
        }
    }
}