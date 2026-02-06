# Bat Markdown Rendering Integration for Qwen CLI

## Overview

This enhancement adds support for rendering markdown responses using the `bat` command-line tool, providing beautiful syntax-highlighted output in the terminal with various color themes.

## Features Implemented

### 1. BatRenderer Utility Class
- **Location**: `src/main/java/com/example/askquery/util/BatRenderer.java`
- **Key Methods**:
  - `isBatAvailable()` - Checks if bat command is available
  - `renderToTerminal(String markdownContent, String theme)` - Renders markdown with specified theme
  - `getAvailableThemes()` - Lists all available bat themes
  - `isThemeAvailable(String theme)` - Validates if a theme exists

### 2. Configuration Options
Added to `AppProperties`:
- `useBatRendering` (boolean) - Enable/disable bat rendering (default: true)
- `batTheme` (String) - Theme to use for bat rendering (default: "Monokai Extended")

### 3. Enhanced InteractiveService
Modified response rendering logic to:
- Check if bat is available and enabled
- Use bat for rendering when available
- Fall back to plain text rendering when bat is unavailable or disabled
- Apply the configured theme

## Available Themes

The integration supports all bat themes including:
- Monokai Extended (default)
- Dracula
- GitHub
- Nord
- OneHalfDark
- Solarized (dark/light)
- And many more...

## Usage Examples

### Enable Bat Rendering (Default)
```bash
java -cp build/libs/qwen_cli-0.1.0.jar com.example.askquery.Main
```

### Custom Theme
```bash
java -Dapp.batTheme="Dracula" -cp build/libs/qwen_cli-0.1.0.jar com.example.askquery.Main
```

### Disable Bat Rendering
```bash
java -Dapp.useBatRendering=false -cp build/libs/qwen_cli-0.1.0.jar com.example.askquery.Main
```

## Testing

### Run Bat Demo
```bash
java -cp build/classes/java/main:build/libs/qwen_cli-0.1.0.jar com.example.askquery.demo.BatDemo
```

### Run Full Demonstration Script
```bash
./demo_bat_rendering.sh
```

## Benefits

1. **Beautiful Terminal Output**: Rich syntax highlighting for code blocks
2. **Multiple Theme Support**: Choose from 24+ professional themes
3. **Graceful Degradation**: Falls back to plain text when bat unavailable
4. **Configurable**: Easy to enable/disable and customize themes
5. **Performance**: Minimal overhead with efficient temporary file handling

## Implementation Details

- Uses temporary files for bat processing
- Includes proper cleanup of temporary files
- Configurable timeouts for bat execution
- Thread-safe implementation for parallel mode
- Comprehensive error handling and logging

## Requirements

- `bat` command must be installed (typically via `brew install bat` on macOS)
- Java 17+ runtime environment
- Standard Qwen CLI dependencies

The integration maintains full backward compatibility while enhancing the user experience with professional-grade markdown rendering.