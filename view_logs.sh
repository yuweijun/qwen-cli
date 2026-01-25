#!/bin/bash

# Script to view the latest log file
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOGS_DIR="$PROJECT_ROOT/logs"

if [ ! -d "$LOGS_DIR" ]; then
    echo "Logs directory does not exist: $LOGS_DIR"
    exit 1
fi

LATEST_LOG=$(ls -t "$LOGS_DIR"/app_*.log 2>/dev/null | head -n 1)

if [ -z "$LATEST_LOG" ]; then
    echo "No log files found in $LOGS_DIR"
    exit 1
fi

echo "Viewing latest log file: $LATEST_LOG"
echo "Press Ctrl+C to exit"
echo "----------------------------------------"

tail -f "$LATEST_LOG"