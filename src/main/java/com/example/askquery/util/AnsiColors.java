package com.example.askquery.util;

/**
 * Utility class for ANSI color codes in terminal output
 */
public class AnsiColors {
    
    // Reset
    public static final String RESET = "\u001B[0m";
    
    // Regular Colors
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";
    
    // Bold Colors
    public static final String BLACK_BOLD = "\u001B[30;1m";
    public static final String RED_BOLD = "\u001B[31;1m";
    public static final String GREEN_BOLD = "\u001B[32;1m";
    public static final String YELLOW_BOLD = "\u001B[33;1m";
    public static final String BLUE_BOLD = "\u001B[34;1m";
    public static final String PURPLE_BOLD = "\u001B[35;1m";
    public static final String CYAN_BOLD = "\u001B[36;1m";
    public static final String WHITE_BOLD = "\u001B[37;1m";
    
    // Background Colors
    public static final String BLACK_BACKGROUND = "\u001B[40m";
    public static final String RED_BACKGROUND = "\u001B[41m";
    public static final String GREEN_BACKGROUND = "\u001B[42m";
    public static final String YELLOW_BACKGROUND = "\u001B[43m";
    public static final String BLUE_BACKGROUND = "\u001B[44m";
    public static final String PURPLE_BACKGROUND = "\u001B[45m";
    public static final String CYAN_BACKGROUND = "\u001B[46m";
    public static final String WHITE_BACKGROUND = "\u001B[47m";
    
    // Prompt styling constants
    public static final String PROMPT_HEADER_COLOR = GREEN_BOLD;
    public static final String PROMPT_TEXT_COLOR = GREEN;
    public static final String PROMPT_DIVIDER_COLOR = GREEN;
    public static final String PROMPT_MODE_HEADER_COLOR = BLUE_BOLD;
    public static final String PROMPT_SECTION_HEADER_COLOR = CYAN_BOLD;
    public static final String PROMPT_NAVIGATION_COLOR = YELLOW;
    public static final String PROMPT_ERROR_COLOR = RED_BOLD;
    public static final String PROMPT_SUCCESS_COLOR = GREEN_BOLD;
    public static final String PROMPT_INFO_COLOR = WHITE;
    
    /**
     * Apply color to text
     * @param text text to colorize
     * @param color ANSI color code
     * @return colorized text
     */
    public static String colorize(String text, String color) {
        return color + text + RESET;
    }
    
    /**
     * Apply bold color to text
     * @param text text to colorize
     * @param color ANSI bold color code
     * @return bold colorized text
     */
    public static String bold(String text, String color) {
        return color + text + RESET;
    }
    
    /**
     * Colorize index numbers with cyan color for better visibility
     * @param index the index number to colorize
     * @return formatted colored index string like "[1]"
     */
    public static String colorizeIndex(int index) {
        return colorize("[" + index + "]", CYAN_BOLD);
    }
    
    /**
     * Colorize index numbers with yellow color as alternative option
     * @param index the index number to colorize
     * @return formatted colored index string like "[1]"
     */
    public static String colorizeIndexYellow(int index) {
        return colorize("[" + index + "]", YELLOW_BOLD);
    }
    
    /**
     * Style for main prompt headers
     * @param text header text
     * @return colorized header
     */
    public static String promptHeader(String text) {
        return colorize(text, PROMPT_HEADER_COLOR);
    }
    
    /**
     * Style for main prompt text
     * @param text prompt text
     * @return colorized prompt text
     */
    public static String promptText(String text) {
        return colorize(text, PROMPT_TEXT_COLOR);
    }
    
    /**
     * Style for divider lines
     * @param text divider text (usually ========)
     * @return colorized divider
     */
    public static String promptDivider(String text) {
        return colorize(text, PROMPT_DIVIDER_COLOR);
    }
    
    /**
     * Style for mode headers (Search Mode, History Mode)
     * @param text mode header text
     * @return colorized mode header
     */
    public static String promptModeHeader(String text) {
        return colorize(text, PROMPT_MODE_HEADER_COLOR);
    }
    
    /**
     * Style for section headers (Search Results, History Entry)
     * @param text section header text
     * @return colorized section header
     */
    public static String promptSectionHeader(String text) {
        return colorize(text, PROMPT_SECTION_HEADER_COLOR);
    }
    
    /**
     * Style for navigation prompts
     * @param text navigation text
     * @return colorized navigation text
     */
    public static String promptNavigation(String text) {
        return colorize(text, PROMPT_NAVIGATION_COLOR);
    }
    
    /**
     * Style for error messages
     * @param text error text
     * @return colorized error text
     */
    public static String promptError(String text) {
        return colorize(text, PROMPT_ERROR_COLOR);
    }
    
    /**
     * Style for success messages
     * @param text success text
     * @return colorized success text
     */
    public static String promptSuccess(String text) {
        return colorize(text, PROMPT_SUCCESS_COLOR);
    }
    
    /**
     * Style for informational messages
     * @param text info text
     * @return colorized info text
     */
    public static String promptInfo(String text) {
        return colorize(text, PROMPT_INFO_COLOR);
    }
}