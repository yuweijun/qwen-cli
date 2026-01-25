# ask-query-java

Spring Boot + Gradle implementation of the interactive ask_query client.

Features:
- Interactive CLI with history and arrow-keys (via JLine)
- Persistent history file (~/.ask_query_history by default)
- Exit commands (`exit`, `quit`)
- Context preservation (keep last N rounds)
- Parallel requests (async) with configurable concurrency
- Navigation through conversation history with `:nav`, `:view`, or `navi` commands and arrow keys
- Calls a configurable API endpoint via Dashscope SDK (updated to use official SDK)

Run:
1. Configure application.yml or environment variables:
    - dashscope.api.key (API key) or set env DASHSCOPE_API_KEY and map it in application.yml
    - app.history-file (optional)
2. Build and run:
   ./gradlew bootRun
   or
   ./gradlew build
   java -jar build/libs/qwen-project-0.1.0.jar

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
- Navigation through conversation history is available using `:nav`, `:view`, or `navi` commands followed by left/right arrow keys
- The application logs output to the `logs/` directory when using the provided scripts