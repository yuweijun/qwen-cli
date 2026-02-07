package com.example.askquery.util;

/**
 * Utility class for filename operations
 */
public class FilenameUtils {
    
    /**
     * Sanitize a string to be used as a filename
     * Extracts first 35 characters and replaces invalid characters with underscores
     * @param input the string to sanitize
     * @return sanitized filename string
     */
    public static String sanitizeFilename(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        
        // Extract first 35 characters
        String truncated = input.length() > 35 ? input.substring(0, 35) : input;
        
        // Replace invalid filename characters with underscores
        // Allow Chinese characters, English letters, digits, and underscores
        return truncated.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "_");
    }
}