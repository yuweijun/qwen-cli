package com.example.askquery.config;

public class AppProperties {
    private String historyFile;
    private int contextLength = 6;
    private boolean parallel = false;
    private int concurrency = 2;
    private String exitCommands = "exit,quit,q";
    private String systemMessage = "You are a helpful assistant.";

    public String getHistoryFile() {
        return historyFile;
    }

    public void setHistoryFile(String historyFile) {
        this.historyFile = historyFile;
    }

    public int getContextLength() {
        return contextLength;
    }

    public void setContextLength(int contextLength) {
        this.contextLength = contextLength;
    }

    public boolean isParallel() {
        return parallel;
    }

    public void setParallel(boolean parallel) {
        this.parallel = parallel;
    }

    public int getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(int concurrency) {
        this.concurrency = concurrency;
    }

    public String getExitCommands() {
        return exitCommands;
    }

    public void setExitCommands(String exitCommands) {
        this.exitCommands = exitCommands;
    }

    public String getSystemMessage() {
        return systemMessage;
    }

    public void setSystemMessage(String systemMessage) {
        this.systemMessage = systemMessage;
    }
}