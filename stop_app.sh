#!/bin/bash
# Script to stop the AskQuery application

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_FILE="$PROJECT_ROOT/build/libs/qwen-project-0.1.0.jar"

# Find and kill the Java process running our application
PIDS=$(ps aux | grep "java.*-cp.*$(basename "$JAR_FILE")" | grep -v grep | awk '{print $2}')

if [ -z "$PIDS" ]; then
    echo "No running AskQuery application found"
else
    echo "Stopping AskQuery application(s) with PID(s): $PIDS"
    kill $PIDS
    echo "Application(s) stopped"
fi
