#!/bin/bash

echo "Setting up recommended extensions for the Qwen CLI project..."

echo "
Recommended VS Code Extensions for this Java 17 + Gradle project:

1. Java Extension Pack
   - Provides Java language support, debugger, test runner, formatter, etc.
   - Install: code --install-extension vscjava.vscode-java-pack

2. Gradle for Java
   - Provides Gradle integration for VS Code
   - Install: code --install-extension vscjava.vscode-gradle

3. GitLens
   - Supercharges Git capabilities within VS Code
   - Install: code --install-extension eamodio.gitlens

4. GitHub Copilot
   - AI-powered code completion
   - Install: code --install-extension GitHub.copilot

5. Rainbow Brackets
   - Highlights matching brackets with colors
   - Install: code --install-extension 2gua.rainbow-brackets

6. Prettier
   - Code formatter supporting multiple languages
   - Install: code --install-extension esbenp.prettier-vscode

7. Error Lens
   - Enhances error and warning display
   - Install: code --install-extension usernamehw.errorlens

To install all at once, run the following command in your terminal:
code --install-extension vscjava.vscode-java-pack &&
code --install-extension vscjava.vscode-gradle &&
code --install-extension eamodio.gitlens &&
code --install-extension GitHub.copilot &&
code --install-extension 2gua.rainbow-brackets &&
code --install-extension esbenp.prettier-vscode &&
code --install-extension usernamehw.errorlens

Additional project setup:
- Make sure Java 17 is installed and JAVA_HOME is set
- Verify Gradle is installed (or use ./gradlew wrapper)
- Set DASHSCOPE_API_KEY environment variable for API access
- Run './gradlew build' to build the project
- Run './gradlew run' to run the application

For more information about the project, check the README.md file.
"

echo "Setup instructions created. You can copy and run the commands above to install the recommended extensions."