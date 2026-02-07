package com.example.askquery.service;

import com.example.askquery.config.AppProperties;
import com.example.askquery.model.HistoryEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SearchHistoryServiceTest {

    private HistoryManager historyManager;
    private AppProperties appProps;
    private SearchHistoryService searchHistoryService;

    @BeforeEach
    public void setUp() {
        historyManager = mock(HistoryManager.class);
        appProps = new AppProperties();
        appProps.setHistoryDisplayCount(15); // Updated to match new default
        searchHistoryService = new SearchHistoryService(historyManager, appProps);
    }

    @Test
    public void testSearchHistoryWithMatchingKeywords() {
        // Given
        List<HistoryEntry> mockEntries = Arrays.asList(
            new HistoryEntry("How to use Java streams?", "Java streams provide functional programming capabilities..."),
            new HistoryEntry("What is Spring Boot?", "Spring Boot is a framework for building Java applications..."),
            new HistoryEntry("Java collections tutorial", "Collections in Java provide data structures...")
        );

        when(historyManager.loadHistory()).thenReturn(mockEntries);

        // When
        List<HistoryEntry> results = searchHistoryService.searchHistory("Java");

        // Then
        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(entry -> entry.getQuestion().contains("Java streams")));
        assertTrue(results.stream().anyMatch(entry -> entry.getQuestion().contains("Java collections")));
        verify(historyManager).loadHistory();
    }

    @Test
    public void testSearchHistoryWithMultipleKeywords() {
        // Given
        List<HistoryEntry> mockEntries = Arrays.asList(
            new HistoryEntry("How to use Java streams?", "Java streams provide functional programming capabilities..."),
            new HistoryEntry("What is Spring Boot?", "Spring Boot is a framework for building Java applications..."),
            new HistoryEntry("Java Spring tutorial", "This tutorial covers Spring framework with Java...")
        );

        when(historyManager.loadHistory()).thenReturn(mockEntries);

        // When
        List<HistoryEntry> results = searchHistoryService.searchHistory("Java Spring");

        // Then
        assertEquals(1, results.size());
        assertEquals("Java Spring tutorial", results.get(0).getQuestion());
        verify(historyManager).loadHistory();
    }

    @Test
    public void testSearchHistoryWithNoMatches() {
        // Given
        List<HistoryEntry> mockEntries = Arrays.asList(
            new HistoryEntry("How to use Python?", "Python is a programming language..."),
            new HistoryEntry("What is Django?", "Django is a Python web framework...")
        );

        when(historyManager.loadHistory()).thenReturn(mockEntries);

        // When
        List<HistoryEntry> results = searchHistoryService.searchHistory("Java");

        // Then
        assertTrue(results.isEmpty());
        verify(historyManager).loadHistory();
    }

    @Test
    public void testSearchHistoryWithEmptyKeywords() {
        // Given
        List<HistoryEntry> mockEntries = Arrays.asList(
            new HistoryEntry("How to use Java?", "Java is a programming language...")
        );

        when(historyManager.loadHistory()).thenReturn(mockEntries);

        // When
        List<HistoryEntry> results1 = searchHistoryService.searchHistory("");
        List<HistoryEntry> results2 = searchHistoryService.searchHistory(null);
        List<HistoryEntry> results3 = searchHistoryService.searchHistory("   ");

        // Then
        assertTrue(results1.isEmpty());
        assertTrue(results2.isEmpty());
        assertTrue(results3.isEmpty());
        // Note: loadHistory() won't be called for empty/null keywords due to early return
    }

    @Test
    public void testSearchHistoryReturnsTop10Results() {
        // Given
        HistoryEntry[] entries = new HistoryEntry[15];
        for (int i = 0; i < 15; i++) {
            entries[i] = new HistoryEntry("Question " + i + " about Java", "Answer " + i);
        }

        when(historyManager.loadHistory()).thenReturn(Arrays.asList(entries));

        // When
        List<HistoryEntry> results = searchHistoryService.searchHistory("Java");

        // Then
        assertEquals(10, results.size()); // Should limit to top 10
        verify(historyManager).loadHistory();
    }

    @Test
    public void testMatchesKeywords() {
        // This tests the private method indirectly through the public search method
        List<HistoryEntry> mockEntries = Arrays.asList(
            new HistoryEntry("Java programming tutorial", "Learn Java programming..."),
            new HistoryEntry("Python programming basics", "Learn Python basics...")
        );

        when(historyManager.loadHistory()).thenReturn(mockEntries);

        // Test case sensitivity handling
        List<HistoryEntry> results1 = searchHistoryService.searchHistory("JAVA");
        List<HistoryEntry> results2 = searchHistoryService.searchHistory("java");

        assertEquals(1, results1.size());
        assertEquals(1, results2.size());
        assertEquals(results1.get(0).getQuestion(), results2.get(0).getQuestion());
    }
}