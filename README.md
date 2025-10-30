# RepoGate IntelliJ Plugin

[![Build Status](https://travis-ci.org/your-username/repogate-plugin.svg?branch=main)](https://travis-ci.org/your-username/repogate-plugin)
[![Version](https://img.shields.io/jetbrains/plugin/v/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)

RepoGate is a powerful IntelliJ IDEA plugin that acts as a gatekeeper for your project's dependencies. It intercepts the addition of new dependencies and validates them against your organization's security policies through the RepoGate API.

This plugin helps prevent the introduction of vulnerable or unapproved packages into your codebase, ensuring a more secure development lifecycle.

## Features

- **Dependency Monitoring**: Automatically detects changes to `package.json` (npm), `pom.xml` (Maven), and `build.gradle` (Gradle) files.
- **Security Validation**: Sends new dependencies to the RepoGate API for security and compliance checks.
- **Installation Blocking**: Prevents the installation of dependencies until they are approved by your security team.
- **Real-time Status Updates**: Polls the RepoGate API for approval status and provides instant notifications.
- **Quick Removal**: Offers a one-click option to remove denied dependencies from your project.
- **Configurable API Token**: Securely store your RepoGate API token for authentication.

## Installation

1.  **Install from Marketplace (Coming Soon!)**: Once published, you will be able to install RepoGate directly from the JetBrains Marketplace.
2.  **Manual Installation**:
    *   Download the latest `RepoGate-*.zip` file from the [releases page](https://github.com/your-username/repogate-plugin/releases).
    *   Open IntelliJ IDEA and go to `Settings` > `Plugins`.
    *   Click the gear icon and select `Install Plugin from Disk...`.
    *   Choose the downloaded ZIP file and restart the IDE.

## Configuration

1.  After installation, open IntelliJ IDEA settings (`File` > `Settings`).
2.  Navigate to `Tools` > `RepoGate`.
3.  Enter your RepoGate API token. You can obtain a token by signing up at [RepoGate.io](https://repogate.io).
4.  (Optional) Configure the RepoGate API URL if you are using a self-hosted instance.
5.  Click `Apply` to save the settings.

## Usage

Once installed and configured, RepoGate works automatically in the background.

1.  When you add a new dependency to your `package.json`, `pom.xml`, or `build.gradle` file, RepoGate will detect the change.
2.  A request will be sent to the RepoGate API to validate the new dependency.
3.  You will receive a notification indicating that the dependency is pending approval.
4.  RepoGate will periodically check for the approval status.
5.  **If Approved**: You will receive a notification, and the dependency will be allowed in your project.
6.  **If Denied**: You will be prompted with a notification to remove the dependency. You can choose to remove it automatically or keep it.

## Contributing

Contributions are welcome! Please feel free to submit a pull request or open an issue for any bugs or feature requests.

1.  Fork the repository.
2.  Create a new branch (`git checkout -b feature/your-feature`).
3.  Make your changes and commit them (`git commit -m 'Add some feature'`).
4.  Push to the branch (`git push origin feature/your-feature`).
5.  Open a pull request.

## License

This project is licensed under the [MIT License](LICENSE).
