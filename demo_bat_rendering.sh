#!/bin/bash

echo "=== Qwen CLI Bat Rendering Demonstration ==="
echo

# Build the project first
echo "Building project..."
./gradlew build > /dev/null 2>&1

if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi

echo "Project built successfully."
echo

# Test bat availability
echo "Testing bat availability..."
if command -v /usr/local/bin/bat &> /dev/null; then
    echo "✓ Bat is available"
else
    echo "✗ Bat is not available"
    echo "Please install bat: brew install bat"
    exit 1
fi

echo
echo "Available bat themes:"
/usr/local/bin/bat --list-themes | head -10

echo
echo "=== Testing Bat Renderer Integration ==="
echo

# Run the bat demo
echo "Running bat demo..."
java -cp build/classes/java/main:build/libs/qwen_cli-0.1.0.jar com.example.askquery.demo.BatDemo

echo
echo "=== Configuration Instructions ==="
echo
echo "To use bat rendering in Qwen CLI, you can set these system properties:"
echo "  -Dapp.useBatRendering=true          # Enable bat rendering (default: true)"
echo "  -Dapp.batTheme=\"Monokai Extended\"   # Choose bat theme (default: \"Monokai Extended\")"
echo
echo "Example usage:"
echo "  java -Dapp.useBatRendering=true -Dapp.batTheme=\"Dracula\" -cp build/libs/qwen_cli-0.1.0.jar com.example.askquery.Main"
echo
echo "=== Demonstration Complete ==="