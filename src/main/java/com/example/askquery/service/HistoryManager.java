package com.example.askquery.service;

import com.example.askquery.model.HistoryEntry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class HistoryManager {
    private final String historyFilePath;
    private final ObjectMapper objectMapper;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final int maxHistorySize;
    
    public HistoryManager(String historyFilePath, int maxHistorySize) {
        this.historyFilePath = historyFilePath;
        this.maxHistorySize = maxHistorySize;
        
        // Initialize ObjectMapper with pretty printing
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        // Ensure the history file exists
        ensureHistoryFile();
    }
    
    /**
     * Ensures the history file exists, creating it with an empty array if necessary
     */
    private void ensureHistoryFile() {
        try {
            Path path = Paths.get(historyFilePath);
            Path parent = path.getParent();
            
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            
            if (!Files.exists(path)) {
                Files.write(path, "[]".getBytes());
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not create history file: " + e.getMessage());
        }
    }
    
    /**
     * Loads all history entries from the JSON file
     */
    public List<HistoryEntry> loadHistory() {
        lock.readLock().lock();
        try {
            File file = new File(historyFilePath);
            if (!file.exists() || file.length() == 0) {
                return new ArrayList<>();
            }
            
            return objectMapper.readValue(file, new TypeReference<List<HistoryEntry>>() {});
        } catch (IOException e) {
            System.err.println("Warning: Could not load history file: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Adds a new entry to the history, maintaining the maximum size limit
     */
    public void addEntry(HistoryEntry entry) {
        lock.writeLock().lock();
        try {
            List<HistoryEntry> history = loadHistory();
            
            // Add the new entry
            history.add(entry);
            
            // Keep only the latest maxHistorySize entries
            if (history.size() > maxHistorySize) {
                int startIndex = history.size() - maxHistorySize;
                history = history.subList(startIndex, history.size());
            }
            
            // Save the updated history back to the file
            saveHistory(history);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Saves the entire history list to the JSON file
     */
    private void saveHistory(List<HistoryEntry> history) {
        try {
            objectMapper.writeValue(new File(historyFilePath), history);
        } catch (IOException e) {
            System.err.println("Warning: Could not save history file: " + e.getMessage());
        }
    }
    
    /**
     * Clears all history entries
     */
    public void clearHistory() {
        lock.writeLock().lock();
        try {
            saveHistory(Collections.emptyList());
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Gets the current history file path
     */
    public String getHistoryFilePath() {
        return historyFilePath;
    }
    
    /**
     * Gets the maximum number of history entries allowed
     */
    public int getMaxHistorySize() {
        return maxHistorySize;
    }
}