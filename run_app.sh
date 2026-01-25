#!/bin/bash

# Script to run the AskQuery Java application in foreground for interactive use
# Arrow keys and other interactive features work properly in foreground mode
# Note: For full interactive functionality, the application runs directly in terminal
# without piping through tee to preserve terminal capabilities

# Set the project root directory
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Set the logs directory
LOGS_DIR="$PROJECT_ROOT/logs"
JAR_FILE="$PROJECT_ROOT/build/libs/qwen-project-0.1.0.jar"

# Create logs directory if it doesn't exist
mkdir -p "$LOGS_DIR"

# Set log file with timestamp
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="$LOGS_DIR/app_$TIMESTAMP.log"

# Print startup information
echo "Starting AskQuery Application in interactive mode..."
echo "Log file: $LOG_FILE (logging starts after exit)"
echo "JAR file: $JAR_FILE"
echo "----------------------------------------"

# Check if JAR file exists
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: JAR file not found at $JAR_FILE"
    echo "Please run './gradlew build' first to build the project."
    exit 1
fi

# Run the Java application in interactive mode with logging
# Using 'script' command to capture output while preserving terminal interactivity
script -q "$LOG_FILE" -c "java -cp '$JAR_FILE' com.example.askquery.Main"

# Print completion message
echo "----------------------------------------"
echo "Application stopped at $(date)"
echo "Check logs in: $LOG_FILE"

