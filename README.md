# ask-query-java

Java + Gradle implementation of the interactive ask_query client.

Features:
- Interactive CLI with history and arrow-keys (via JLine)
- Persistent history file (~/.qwen_query_history by default)
- Exit commands (`exit`, `quit`)
- Context preservation (keep last N rounds)
- Parallel requests (async) with configurable concurrency
- View conversation history with `:h` command (also supports `:history`)
- Calls a configurable API endpoint via Dashscope SDK (updated to use official SDK)

Run:
1. Set environment variable:
    - DASHSCOPE_API_KEY (API key)
2. Build and run:
   ./gradlew run
   or
   ./gradlew build
   java -cp build/libs/qwen-project-0.1.0.jar com.example.askquery.Main

## Scripts

The project includes convenient scripts for running the application:

### Quick Start with Scripts:
1. Build the project: `./gradlew build`
2. Run with logging: `./run_app.sh` (foreground) or `./start_app.sh` (background)
3. View logs: `./view_logs.sh`
4. Stop background app: `./stop_app.sh`

See SCRIPTS_README.md for detailed instructions on using the scripts.

Notes:
- The code now uses the official Dashscope Java SDK for API calls
- The application logs output to the `logs/` directory when using the provided scripts
- Configuration can be set via system properties (e.g., -Ddashscope.api.key=your_key)