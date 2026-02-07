# AGENTS.md

This file provides guidance to Qoder (qoder.com) when working with code in this repository.

## Project Overview

Qwen CLI is a Java-based interactive command-line application that provides access to the Qwen AI model through the DashScope API. The application maintains conversation context, persistent history, and supports both sequential and parallel request modes.

## Code Style Guidelines

- **Java code line wrapping**: Use 120 characters maximum line length
- **Indentation**: 4 spaces for Java code
- **Naming conventions**: Follow standard Java camelCase and PascalCase conventions
- **Imports**: Use static imports when appropriate, avoid wildcard imports (*). **Prefer using import statements over fully qualified package names** - use `import com.example.ClassName` instead of `com.example.ClassName` throughout the code
- **POJO methods**: DO NOT use one-line style for setter/getter methods
- **Class properties**: Add blank line after every property in Java classes
- **Build tool**: Use Gradle as the build tool for Java projects
- **Web framework**: Use Spring Boot 3 latest version for web Java projects
- **Testing framework**: Use JUnit 5 and Mockito to generate test cases

### Git Commit Guidelines

**Do not add AI attribution** to commit messages such as:
- `co-authored-by: AI Assistant`
- `Generated with [AI Tool Name]`
- `AI-assisted commit`
- Any other AI-generated attribution metadata

Keep commit messages focused on describing the actual changes made to the codebase.

### Import Management

**Always remove unused imports** after making changes to classes. This includes:
- Imports that were used previously but are no longer needed
- Auto-imported classes that aren't actually used in the code
- Wildcard imports that should be replaced with specific imports

Run `./gradlew build` to verify all imports are properly used and there are no compilation warnings about unused imports.

### Test Method Naming Convention

Follow the BDD (Behavior-Driven Development) style for JUnit test method names using the pattern:
`given_[conditions]_when_[action]_then_[expected_outcome]`

Examples:
- `given_valid_input_when_process_data_then_return_expected_result`
- `given_null_parameter_when_execute_method_then_throw_exception`
- `given_empty_list_when_find_items_then_return_empty_result`

This naming convention makes tests self-documenting and clearly expresses:
- **Given**: The preconditions or setup
- **When**: The action being tested
- **Then**: The expected outcome or behavior

## Build and Run Commands

### Building
```bash
./gradlew build
```

### Running
```bash
# Run with Gradle
export DASHSCOPE_API_KEY=your_api_key_here
./gradlew run

# Run JAR directly
java -cp build/libs/qwen_cli-0.1.0.jar com.example.askquery.Main

# With bat markdown rendering (enabled by default)
java -Dapp.useBatRendering=true -Dapp.batTheme="Monokai Extended" -cp build/libs/qwen_cli-0.1.0.jar com.example.askquery.Main

# With custom bat command path
java -Dapp.batCommand="/opt/homebrew/bin/bat" -cp build/libs/qwen_cli-0.1.0.jar com.example.askquery.Main

# With custom history display count
java -Dapp.historyDisplayCount=50 -cp build/libs/qwen_cli-0.1.0.jar com.example.askquery.Main

# With custom configuration
java -Dapp.contextLength=10 -Ddashscope.model=qwen-max -cp build/libs/qwen_cli-0.1.0.jar com.example.askquery.Main
```

### Testing
```bash
./gradlew test
```

## Architecture

### Core Components

**Main Entry Point** (`Main.java`)
- Loads configuration from system properties and environment variables
- Creates and wires service instances
- Accepts optional initial query as command-line argument

**InteractiveService** (`service/InteractiveService.java`)
- Orchestrates the CLI interaction loop using JLine for terminal handling
- Manages conversation state with a deque structure for context preservation
- Handles two types of history:
  - JLine history: stores user input in `~/.qwen_jline_history.txt` for arrow-key navigation
  - JSON history: stores Q&A pairs via HistoryManager for session persistence
- Supports parallel mode using ExecutorService with configurable concurrency
- Implements history caching: checks JSON history before making API calls to reuse existing answers
- Saves each Q&A to individual markdown files in `questions/` directory

**DashscopeClient** (`service/DashscopeClient.java`)
- Wraps the official Alibaba DashScope SDK
- Converts internal message format to SDK Message objects
- Extracts response text from SDK's nested JSON structure: `output.choices[0].message.content`
- Returns responses as JsonNode for flexibility

**HistoryManager** (`service/HistoryManager.java`)
- Thread-safe JSON-based history persistence using ReentrantReadWriteLock
- Maintains up to 100 most recent Q&A entries (configurable)
- Uses Jackson for JSON serialization with pretty printing enabled
- Automatically creates history file with empty array if missing

### Configuration System

Configuration is loaded via system properties with fallback to defaults:

**App Properties** (`config/AppProperties.java`)
- `app.historyFile`: Path to JSON history file (default: `~/.qwen_cli_history.json`)
- `app.contextLength`: Number of conversation rounds to keep (default: 6, means 12 messages total)
- `app.parallel`: Enable concurrent requests (default: false)
- `app.concurrency`: Thread pool size for parallel mode (default: 2)
- `app.exitCommands`: Comma-separated exit commands (default: "exit,quit,q")
- `app.systemMessage`: AI system prompt (default: "You are a helpful assistant.")
- `app.useBatRendering`: Enable bat markdown rendering (default: true)
- `app.batTheme`: Theme for bat rendering (default: "Monokai Extended")
- `app.batCommand`: Path to bat command (default: "/usr/local/bin/bat")
- `app.historyDisplayCount`: Number of latest history items to display per page (default: 15)

**DashScope Properties** (`config/DashscopeProperties.java`)
- `dashscope.api.key`: API key (falls back to `DASHSCOPE_API_KEY` env var)
- `dashscope.model`: Model name (default: "qwen-plus")

### Data Flow

1. User input is captured via JLine LineReader
2. InteractiveService checks HistoryManager for cached answer
3. If cache miss, builds message list including system message and conversation context
4. DashscopeClient converts to SDK format and calls API
5. Response is added to conversation deque (maintaining context window)
6. HistoryManager persists Q&A to JSON file
7. Individual question is saved to dated markdown file in `questions/`

### Context Management

The application maintains conversation context using a sliding window:
- Conversation stored in deque as alternating user/assistant message pairs
- Context window is 2 * contextLength (user + assistant for each round)
- Oldest messages are removed when window size is exceeded
- System message is always prepended to API calls but not counted in context

## Project Structure

```
src/main/java/com/example/askquery/
├── Main.java                          # Entry point, configuration loading
├── config/
│   ├── AppProperties.java            # Application settings
│   └── DashscopeProperties.java      # API client settings
├── model/
│   └── HistoryEntry.java             # Q&A data model
├── service/
│   ├── DashscopeClient.java          # API client wrapper
│   ├── HistoryManager.java           # JSON history persistence
│   └── InteractiveService.java       # Main CLI loop and orchestration
└── util/
    ├── BatRenderer.java              # Bat command integration for markdown rendering
    └── MarkdownRenderer.java         # HTML markdown rendering utilities
```

## Dependencies

- **dashscope-sdk-java**: Official Alibaba DashScope API SDK
- **jline:3.23.0**: Terminal handling, history support, arrow-key navigation
- **jackson-databind**: JSON processing for history files
- **JUnit 5 & Mockito**: Testing framework

## Runtime Artifacts

- `logs/`: Application logs directory (created automatically)
- `questions/`: Individual Q&A markdown files named `{date}_{first35chars}.md`
- `~/.qwen_cli_history.json`: JSON history file with Q&A pairs
- `~/.qwen_jline_history.txt`: JLine command history for terminal navigation
