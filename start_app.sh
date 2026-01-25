#!/bin/bash

# Script to start the AskQuery Java application in the background
# Logs output to the logs directory in the project root

# Set the project root directory
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Set the logs directory
LOGS_DIR="$PROJECT_ROOT/logs"
JAR_FILE="$PROJECT_ROOT/build/libs/qwen_cli-0.1.0.jar"

# Create logs directory if it doesn't exist
mkdir -p "$LOGS_DIR"

# Set log file with timestamp
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="$LOGS_DIR/app_background_$TIMESTAMP.log"

# Check if JAR file exists
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: JAR file not found at $JAR_FILE"
    echo "Please run './gradlew build' first to build the project."
    exit 1
fi

# Start the application in the background
nohup java -cp "$JAR_FILE" com.example.askquery.Main > "$LOG_FILE" 2>&1 &

# Save the process ID
APP_PID=$!

# Print startup information
echo "AskQuery Application started in background!"
echo "PID: $APP_PID"
echo "Log file: $LOG_FILE"
echo "To stop the application, run: kill $APP_PID"

# Also create a simple stop script
STOP_SCRIPT="$PROJECT_ROOT/stop_app.sh"
cat > "$STOP_SCRIPT" << EOF
#!/bin/bash
# Script to stop the AskQuery application

PROJECT_ROOT="\$(cd "\$(dirname "\${BASH_SOURCE[0]}")" && pwd)"
JAR_FILE="\$PROJECT_ROOT/build/libs/qwen_cli-0.1.0.jar"

# Find and kill the Java process running our application
PIDS=\$(ps aux | grep "java.*-cp.*\$(basename "\$JAR_FILE")" | grep -v grep | awk '{print \$2}')

if [ -z "\$PIDS" ]; then
    echo "No running AskQuery application found"
else
    echo "Stopping AskQuery application(s) with PID(s): \$PIDS"
    kill \$PIDS
    echo "Application(s) stopped"
fi
EOF

chmod +x "$STOP_SCRIPT"