# Qwen CLI

Qwen CLI is an interactive command-line interface application that provides access to the Qwen AI model through the DashScope API. Built with Java and Gradle, it offers a convenient way to interact with AI models directly from your terminal.

## Features

- **Interactive CLI**: Full-featured command-line interface with history support and arrow-key navigation (via JLine)
- **Persistent History**: Conversation history saved to `~/.qwen_cli_history` by default
- **Context Preservation**: Maintains context from previous conversations (configurable number of rounds)
- **Conversation History**: View all conversation history with `h` commands
- **Parallel Requests**: Support for asynchronous parallel requests with configurable concurrency
- **Configurable API Endpoint**: Uses the official Dashscope Java SDK for API calls
- **Flexible Configuration**: Multiple ways to configure the application via environment variables, system properties, or defaults

## Prerequisites

- Java 17 or higher
- Gradle (for building the project)
- DashScope API Key from Alibaba Cloud

## Setup

1. **Get API Key**: Obtain your DashScope API key from [Alibaba Cloud Console](https://dashscope.console.aliyun.com/)

2. **Set Environment Variable**:
   ```bash
   export DASHSCOPE_API_KEY=your_api_key_here
   ```

## Building the Project

```bash
# Clone the repository (if needed)
git clone <repository-url>
cd qwen-cli

# Build the project
./gradlew build
```

## Running the Application

### Method 1: Direct Gradle Command

```bash
# Set API key and run directly with Gradle
export DASHSCOPE_API_KEY=your_api_key_here
./gradlew run
```

### Method 2: Direct JAR Execution

```bash
# Build first
./gradlew build

# Run the JAR directly
java -cp build/libs/qwen_cli-0.1.0.jar com.example.askquery.Main

# Or with API key as system property
java -Ddashscope.api.key=your_api_key -cp build/libs/qwen_cli-0.1.0.jar com.example.askquery.Main
```

## Configuration Options

The application can be configured using system properties. Here are the available options:

| Property | Description | Default Value |
|----------|-------------|---------------|
| `app.historyFile` | Path to history file | `~/.qwen_cli_history` |
| `app.contextLength` | Number of conversation rounds to keep in context | `6` |
| `app.parallel` | Enable parallel requests | `false` |
| `app.concurrency` | Number of concurrent requests when parallel enabled | `2` |
| `app.exitCommands` | Comma-separated list of exit commands | `exit,quit,q` |
| `app.systemMessage` | Initial system message for the AI | `"You are a helpful assistant."` |
| `dashscope.api.key` | DashScope API key | From `DASHSCOPE_API_KEY` env var |
| `dashscope.model` | Model to use | `qwen-plus` |

Example with custom configuration:
```bash
java -Dapp.contextLength=10 -Ddashscope.model=qwen-max -Dapp.historyFile=./my_history.txt -cp build/libs/qwen_cli-0.1.0.jar com.example.askquery.Main
```

## Available Commands

- `:h` or `:history` - View all conversation history (questions and responses)
- `exit`, `quit`, or `q` - Exit the application

## Project Structure

```
qwen-cli/
├── README.md              # This file
├── build.gradle           # Gradle build configuration
├── src/                   # Source code
├── logs/                  # Log files (created automatically)
└── questions/             # Question history files
```

## Dependencies

- [DashScope Java SDK](https://github.com/aliyun/dashscope-sdk-java) - Official SDK for DashScope API
- [JLine](https://github.com/jline/jline3) - Terminal/console handling for interactive features
- [Jackson](https://github.com/FasterXML/jackson) - JSON processing
- Gradle - Build automation

## Logging

The application logs output to the `logs/` directory.

## Troubleshooting

1. **API Key Issues**: Make sure your `DASHSCOPE_API_KEY` environment variable is set correctly
2. **Build Issues**: Ensure you have Java 17+ and run `./gradlew clean build`
3. **History Not Persisting**: Check that the application has write permissions to the history file location

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.