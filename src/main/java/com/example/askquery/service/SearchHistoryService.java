package com.example.askquery.service;

import com.example.askquery.config.AppProperties;
import com.example.askquery.model.HistoryEntry;
import com.example.askquery.util.BatRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Service class to handle search functionality for history entries
 */
public class SearchHistoryService {
    
    private final HistoryManager historyManager;
    private final AppProperties appProps;
    private final Scanner scanner;
    
    public SearchHistoryService(HistoryManager historyManager, AppProperties appProps) {
        this.historyManager = historyManager;
        this.appProps = appProps;
        this.scanner = new Scanner(System.in);
    }
    
    /**
     * Search history entries by keywords and return top 10 matches
     * @param keywords search keywords
     * @return list of matched HistoryEntry objects
     */
    public List<HistoryEntry> searchHistory(String keywords) {
        if (keywords == null || keywords.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String[] searchTerms = keywords.toLowerCase().trim().split("\\s+");
        List<HistoryEntry> allEntries = historyManager.loadHistory();
        
        return allEntries.stream()
                .filter(entry -> entry.getQuestion() != null)
                .filter(entry -> matchesKeywords(entry.getQuestion().toLowerCase(), searchTerms))
                .limit(10)
                .collect(Collectors.toList());
    }
    
    /**
     * Check if text matches all search keywords
     * @param text text to search in
     * @param keywords array of keywords to match
     * @return true if all keywords are found in the text
     */
    private boolean matchesKeywords(String text, String[] keywords) {
        for (String keyword : keywords) {
            if (!text.contains(keyword)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Display search results with 1-based indexing
     * @param results list of HistoryEntry objects to display
     */
    public void displaySearchResults(List<HistoryEntry> results) {
        if (results.isEmpty()) {
            System.out.println("No matching history entries found.");
            return;
        }
        
        System.out.println("\n========== Search Results ==========");
        for (int i = 0; i < results.size(); i++) {
            HistoryEntry entry = results.get(i);
            System.out.printf("[%d] %s%n", i + 1, entry.getQuestion());
        }
        System.out.println("====================================");
        System.out.print("Enter number to view details, 'q' to quit, or press Enter to search again: ");
    }
    
    /**
     * Handle the search interaction loop with consistent navigation controls
     */
    public void handleSearchInteraction() {
        System.out.println("\n=== Search History Mode ===");
        
        while (true) {
            System.out.print("Enter search keywords (or 'q' to quit, press Enter to refresh): ");
            
            String keywords = scanner.nextLine().trim();
            
            if (keywords.equalsIgnoreCase("q")) {
                break; // Exit search mode and return to main menu
            } else if (keywords.isEmpty()) {
                continue; // Refresh - show prompt again
            }
            
            List<HistoryEntry> results = searchHistory(keywords);
            displaySearchResults(results);
            
            if (!results.isEmpty()) {
                if (!handleSelection(results)) {
                    break; // Exit if user presses 'q' during selection
                }
            }
        }
    }
    
    /**
     * Handle user selection of a search result
     * @param results list of search results
     * @return true if should continue in search mode, false if should exit
     */
    private boolean handleSelection(List<HistoryEntry> results) {
        try {
            String input = scanner.nextLine().trim();
            
            if (input.equalsIgnoreCase("q")) {
                return false; // Exit search mode
            } else if (input.isEmpty()) {
                return true; // Stay in search mode - will prompt for keywords again
            }
            
            int selectedIndex = Integer.parseInt(input);
            if (selectedIndex >= 1 && selectedIndex <= results.size()) {
                HistoryEntry selectedEntry = results.get(selectedIndex - 1);
                displayHistoryEntry(selectedEntry);
                return true; // Continue search mode after viewing
            } else {
                System.out.println("Invalid selection. Please enter a number between 1 and " + results.size() + ", 'q' to quit, or press Enter to search again.");
                return true; // Continue search mode
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a valid number, 'q' to quit, or press Enter to search again.");
            return true; // Continue search mode
        }
    }
    
    /**
     * Display a history entry with bat rendering if available
     * @param entry HistoryEntry to display
     */
    private void displayHistoryEntry(HistoryEntry entry) {
        System.out.println("\n========== History Entry ==========");
        System.out.println("Question: " + entry.getQuestion());
        System.out.println("-----------------------------------");
        
        if (entry.getAnswer() != null && !entry.getAnswer().isEmpty()) {
            System.out.println("Answer:");
            // Use bat rendering if available and configured
            if (BatRenderer.isBatAvailable()) {
                if (!BatRenderer.renderToTerminal(entry.getAnswer(), "Monokai Extended")) {
                    // Fallback to plain text if bat rendering fails
                    System.out.println(entry.getAnswer());
                }
            } else {
                System.out.println(entry.getAnswer());
            }
        } else {
            System.out.println("Answer: (No answer available)");
        }
        
        System.out.println("===================================");
        System.out.println("Press Enter to return to continue...");
        scanner.nextLine();
    }
    
    /**
     * Display the latest history entries with 1-based indexing in interactive mode with pagination
     * Number of entries displayed is controlled by appProps.getHistoryDisplayCount()
     */
    public void displayLatestHistory() {
        System.out.println("\n=== History Mode ===");
        
        int currentPage = 0;
        int pageSize = appProps.getHistoryDisplayCount();
        
        while (true) {
            List<HistoryEntry> allEntries = historyManager.loadHistory();
            
            if (allEntries.isEmpty()) {
                System.out.println("No history entries found.");
                System.out.println("Press Enter to refresh or 'q' to quit: ");
                String input = scanner.nextLine().trim();
                if (input.equalsIgnoreCase("q")) {
                    break;
                }
                continue;
            }
            
            // Calculate pagination
            int totalPages = (int) Math.ceil((double) allEntries.size() / pageSize);
            int startIndex = currentPage * pageSize;
            int endIndex = Math.min(startIndex + pageSize, allEntries.size());
            
            // Get current page entries (most recent first within the page)
            List<HistoryEntry> currentPageEntries = allEntries.subList(
                Math.max(0, allEntries.size() - endIndex), 
                Math.max(0, allEntries.size() - startIndex)
            );
            
            System.out.println("\n========== Latest History ==========");
            System.out.printf("Page %d of %d (Total: %d entries)%n", currentPage + 1, totalPages, allEntries.size());
            System.out.println("====================================");
            
            // Display entries with 1-based indexing within current page
            for (int i = 0; i < currentPageEntries.size(); i++) {
                HistoryEntry entry = currentPageEntries.get(i);
                int displayIndex = i + 1; // 1-based index within current page
                System.out.printf("[%d] %s%n", displayIndex, entry.getQuestion());
            }
            
            System.out.println("====================================");
            
            // Show navigation options
            System.out.print("Enter number to view details");
            if (currentPage > 0) {
                System.out.print(", 'p' for previous page");
            }
            if (currentPage < totalPages - 1) {
                System.out.print(", 'n' for next page");
            }
            System.out.print(", 'q' to quit, or press Enter to refresh: ");
            
            String input = scanner.nextLine().trim();
            
            if (input.equalsIgnoreCase("q")) {
                break; // Exit history mode
            } else if (input.equalsIgnoreCase("n") && currentPage < totalPages - 1) {
                currentPage++; // Next page
                continue;
            } else if (input.equalsIgnoreCase("p") && currentPage > 0) {
                currentPage--; // Previous page
                continue;
            } else if (input.isEmpty()) {
                continue; // Refresh current page
            } else {
                // Handle selection
                try {
                    int selectedIndex = Integer.parseInt(input);
                    if (selectedIndex >= 1 && selectedIndex <= currentPageEntries.size()) {
                        // Convert 1-based index to actual index
                        HistoryEntry selectedEntry = currentPageEntries.get(selectedIndex - 1);
                        displayHistoryEntry(selectedEntry);
                    } else {
                        System.out.println("Invalid selection. Please enter a number between 1 and " + currentPageEntries.size());
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input. Please enter a valid number, 'n' for next, 'p' for previous, 'q' to quit, or press Enter to refresh.");
                }
            }
        }
    }
}