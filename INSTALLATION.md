# RepoGate Plugin Installation Guide

This guide will walk you through the process of installing and configuring the RepoGate IntelliJ plugin.

## Prerequisites

- IntelliJ IDEA 2023.2 or later (Community or Ultimate Edition)
- Java 17 or later
- A RepoGate API token (obtain from [RepoGate.io](https://repogate.io))
- A running RepoGate API server (default: `http://localhost:3000/api/v1`)

## Installation Methods

### Method 1: From JetBrains Marketplace (Recommended - Coming Soon)

1. Open IntelliJ IDEA
2. Navigate to `File` > `Settings` > `Plugins`
3. Click on the `Marketplace` tab
4. Search for "RepoGate"
5. Click `Install` and restart IntelliJ IDEA

### Method 2: Manual Installation from ZIP

1. Download the latest `RepoGate-1.0.0.zip` from the `build/distributions/` directory
2. Open IntelliJ IDEA
3. Navigate to `File` > `Settings` > `Plugins`
4. Click the gear icon (⚙️) and select `Install Plugin from Disk...`
5. Select the downloaded ZIP file
6. Click `OK` and restart IntelliJ IDEA

### Method 3: Build from Source

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/repogate-plugin.git
   cd repogate-plugin
   ```

2. Build the plugin:
   ```bash
   ./gradlew buildPlugin
   ```

3. The plugin ZIP will be created in `build/distributions/RepoGate-1.0.0.zip`

4. Follow the manual installation steps above

## Configuration

After installation, you need to configure the plugin with your RepoGate API credentials.

### Step 1: Access Plugin Settings

1. Open IntelliJ IDEA
2. Navigate to `File` > `Settings` (or `IntelliJ IDEA` > `Preferences` on macOS)
3. Go to `Tools` > `RepoGate`

### Step 2: Configure API Settings

You will see the following configuration options:

- **Enable RepoGate**: Toggle to enable or disable the plugin
- **API URL**: The base URL of your RepoGate API server
  - Default: `http://localhost:3000/api/v1`
  - For production: Update to your hosted RepoGate API URL
- **API Token**: Your RepoGate authentication token
  - Obtain this from your RepoGate dashboard at [RepoGate.io](https://repogate.io)

### Step 3: Obtain API Token

1. Visit [RepoGate.io](https://repogate.io)
2. Sign up or log in to your account
3. Navigate to your dashboard
4. Generate a new API token
5. Copy the token

### Step 4: Enter Configuration

1. Paste your API token into the **API Token** field
2. If using a custom API URL, update the **API URL** field
3. Ensure **Enable RepoGate** is checked
4. Click `Apply` then `OK`

## Verifying Installation

To verify that RepoGate is working correctly:

1. Open a project with a `package.json`, `pom.xml`, or `build.gradle` file
2. Add a new dependency to the file
3. Save the file
4. You should see a notification from RepoGate indicating that the dependency is being validated

## Supported Package Managers

RepoGate currently supports the following package managers:

- **npm**: Monitors `package.json` files
- **Maven**: Monitors `pom.xml` files
- **Gradle**: Monitors `build.gradle` and `build.gradle.kts` files

## Troubleshooting

### Plugin Not Detecting Changes

- Ensure the plugin is enabled in settings
- Verify that your API token is correct
- Check that the API URL is accessible from your machine
- Restart IntelliJ IDEA

### API Connection Errors

- Verify that your RepoGate API server is running
- Check your network connection
- Ensure the API URL is correct (include `/api/v1` at the end)
- Verify that your API token is valid

### No Notifications Appearing

- Check IntelliJ IDEA notification settings
- Ensure notifications are not disabled for the RepoGate group
- Look for notifications in the Event Log (`View` > `Tool Windows` > `Event Log`)

## Uninstallation

To uninstall the RepoGate plugin:

1. Navigate to `File` > `Settings` > `Plugins`
2. Find "RepoGate" in the installed plugins list
3. Click the dropdown arrow next to the plugin name
4. Select `Uninstall`
5. Restart IntelliJ IDEA

## Support

For issues, feature requests, or questions:

- GitHub Issues: [https://github.com/your-username/repogate-plugin/issues](https://github.com/your-username/repogate-plugin/issues)
- Email: support@repogate.io
- Documentation: [https://docs.repogate.io](https://docs.repogate.io)
