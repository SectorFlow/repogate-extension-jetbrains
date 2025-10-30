# RepoGate Plugin Development Guide

This guide provides information for developers who want to contribute to or modify the RepoGate IntelliJ plugin.

## Development Environment Setup

### Prerequisites

- Java 17 or later
- Gradle 8.5 or later
- IntelliJ IDEA 2023.2 or later (Community or Ultimate Edition)
- Git

### Clone the Repository

```bash
git clone https://github.com/your-username/repogate-plugin.git
cd repogate-plugin
```

### Open in IntelliJ IDEA

1. Open IntelliJ IDEA
2. Select `File` > `Open`
3. Navigate to the cloned repository directory
4. Select the `build.gradle.kts` file
5. Click `Open as Project`
6. Wait for Gradle to sync and download dependencies

## Project Structure

```
RepoGate/
├── src/
│   └── main/
│       ├── java/
│       │   └── io/
│       │       └── repogate/
│       │           └── plugin/
│       │               ├── api/
│       │               │   └── RepoGateApiClient.java
│       │               ├── listeners/
│       │               │   └── DependencyFileListener.java
│       │               ├── model/
│       │               │   └── DependencyInfo.java
│       │               ├── parser/
│       │               │   ├── DependencyParser.java
│       │               │   ├── GradleDependencyParser.java
│       │               │   ├── MavenDependencyParser.java
│       │               │   └── NpmDependencyParser.java
│       │               ├── service/
│       │               │   └── DependencyValidator.java
│       │               └── settings/
│       │                   ├── RepoGateConfigurable.java
│       │                   ├── RepoGateSettings.java
│       │                   └── RepoGateSettingsComponent.java
│       └── resources/
│           └── META-INF/
│               ├── plugin.xml
│               ├── repogate-gradle.xml
│               └── repogate-maven.xml
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

## Key Components

### 1. DependencyFileListener

Monitors file system changes and detects modifications to dependency files (`package.json`, `pom.xml`, `build.gradle`).

### 2. DependencyParser

Interface for parsing dependencies from different file formats. Implementations:
- `NpmDependencyParser`: Parses `package.json`
- `MavenDependencyParser`: Parses `pom.xml`
- `GradleDependencyParser`: Parses `build.gradle` and `build.gradle.kts`

### 3. RepoGateApiClient

Handles communication with the RepoGate REST API:
- `requestDependency()`: Submits a new dependency for validation
- `checkDependency()`: Polls for approval status

### 4. DependencyValidator

Orchestrates the validation workflow:
- Sends validation requests
- Polls for approval status every 10 seconds
- Shows notifications to the user
- Handles approved/denied responses

### 5. Settings Components

- `RepoGateSettings`: Persists plugin configuration
- `RepoGateConfigurable`: Provides the settings UI
- `RepoGateSettingsComponent`: Builds the settings form

## Building the Plugin

### Build Plugin JAR

```bash
./gradlew build
```

The compiled JAR will be in `build/libs/RepoGate-1.0.0.jar`

### Build Plugin Distribution

```bash
./gradlew buildPlugin
```

The distributable ZIP will be in `build/distributions/RepoGate-1.0.0.zip`

### Clean Build

```bash
./gradlew clean build
```

## Running and Testing

### Run Plugin in Sandbox IDE

```bash
./gradlew runIde
```

This will launch a new IntelliJ IDEA instance with the plugin installed.

### Run Tests

```bash
./gradlew test
```

## API Integration

The plugin communicates with the RepoGate API using the following endpoints:

### POST /api/v1/dependencies/request

Request validation for a new dependency.

**Request Body:**
```json
{
  "packageName": "lodash",
  "packageManager": "npm"
}
```

**Response:**
```json
{
  "approved": false,
  "message": "Package requires approval. A request will be submitted to your security team.",
  "packageName": "lodash",
  "packageManager": "npm"
}
```

### POST /api/v1/dependencies/check

Check the approval status of a dependency.

**Request Body:**
```json
{
  "packageName": "lodash",
  "packageManager": "npm"
}
```

**Response (Pending):**
```json
{
  "approved": false,
  "message": "Package requires approval. A request will be submitted to your security team.",
  "packageName": "lodash",
  "packageManager": "npm",
  "status": "pending"
}
```

**Response (Approved):**
```json
{
  "approved": true,
  "message": "Package has been approved.",
  "packageName": "lodash",
  "packageManager": "npm",
  "status": "approved"
}
```

**Response (Denied):**
```json
{
  "approved": false,
  "message": "Package has been denied due to security vulnerabilities.",
  "packageName": "lodash",
  "packageManager": "npm",
  "status": "denied"
}
```

## Adding Support for New Package Managers

To add support for a new package manager:

1. Create a new parser class implementing `DependencyParser`
2. Implement the required methods:
   - `parseNewDependencies()`
   - `supports()`
   - `getPackageManager()`
3. Add the parser to the `parsers` list in `DependencyFileListener`

Example:

```java
public class YarnDependencyParser implements DependencyParser {
    @Override
    public List<DependencyInfo> parseNewDependencies(String content, String previousContent) {
        // Implementation
    }
    
    @Override
    public boolean supports(String fileName) {
        return "yarn.lock".equals(fileName);
    }
    
    @Override
    public String getPackageManager() {
        return "yarn";
    }
}
```

## Debugging

To debug the plugin:

1. Set breakpoints in your code
2. Run `./gradlew runIde --debug-jvm`
3. In IntelliJ IDEA, create a Remote JVM Debug configuration
4. Set the port to 5005
5. Start debugging

## Code Style

- Follow standard Java conventions
- Use meaningful variable and method names
- Add JavaDoc comments for public methods
- Keep methods focused and concise
- Handle exceptions appropriately

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Test thoroughly
5. Commit your changes (`git commit -m 'Add amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

## Publishing to JetBrains Marketplace

1. Update version in `build.gradle.kts`
2. Update `CHANGELOG.md`
3. Build the plugin: `./gradlew buildPlugin`
4. Sign in to [JetBrains Marketplace](https://plugins.jetbrains.com/)
5. Upload the ZIP file from `build/distributions/`
6. Fill in the plugin details and submit for review

## Resources

- [IntelliJ Platform SDK Documentation](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
- [Gradle IntelliJ Plugin](https://github.com/JetBrains/gradle-intellij-plugin)
- [JetBrains Marketplace](https://plugins.jetbrains.com/)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
