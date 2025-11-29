# RepoGate Plugin for JetBrains IDEs

[![Version](https://img.shields.io/badge/version-2.0.0-blue.svg)](https://github.com/SectorFlow/repogate-extension-jetbrains)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

RepoGate is a security plugin that monitors and validates dependencies before they are added to your project. It integrates with the RepoGate platform to provide real-time security scanning and approval workflows for package dependencies.

## 🚀 Supported IDEs

- **IntelliJ IDEA** (Ultimate & Community)
- **PyCharm** (Professional & Community)
- **WebStorm**
- **PhpStorm**
- **RubyMine**
- **GoLand**
- **CLion**
- **Rider**
- **Android Studio**

## ✨ Features

### Authentication
- **EntraID SSO Authentication** - OAuth2 with PKCE flow for enterprise security
- **API Token Authentication** - Legacy support for simple token-based auth
- **Secure Credential Storage** - Uses IntelliJ PasswordSafe (encrypted)
- **Automatic Token Refresh** - Background token refresh for EntraID sessions
- **Token Rotation** - Support for refresh token rotation

### Dependency Monitoring
- **Real-time Monitoring** - Watches `package.json` (npm), `pom.xml` (Maven), and `build.gradle`/`build.gradle.kts` (Gradle)
- **Version Tracking** - Treats version changes as new packages (triggers re-scan)
- **Removal Detection** - Notifies backend when dependencies are removed
- **Git Repository Detection** - Includes repository context in API calls
- **Dev Dependencies** - Configurable inclusion of development dependencies

### Security Validation
- **Approval Workflow** - Validates dependencies against RepoGate security policies
- **Status Tracking** - Monitors approval status: approved, denied, pending, scanning, not_found
- **Real-time Notifications** - In-IDE notifications for status changes
- **Automatic Polling** - Polls for status updates until final decision
- **Connection Retry** - Automatic retry logic for service availability

### User Experience
- **Tools Menu Integration** - All actions accessible via Tools > RepoGate
- **Status Notifications** - Visual feedback with icons (✓, ✗, ⏳, 🔍, ❓)
- **Account Information** - View current authentication status and user info
- **Connection Testing** - Test API connectivity on demand
- **Manual Scanning** - Force re-scan of all packages

## 📦 Installation

### From JetBrains Marketplace (Recommended)
1. Open your JetBrains IDE
2. Go to **Settings/Preferences** → **Plugins**
3. Search for "RepoGate"
4. Click **Install**
5. Restart the IDE

### Manual Installation
1. Download the latest `RepoGate-2.0.0.zip` from [Releases](https://github.com/SectorFlow/repogate-extension-jetbrains/releases)
2. Open your JetBrains IDE
3. Go to **Settings/Preferences** → **Plugins**
4. Click the gear icon ⚙️ → **Install Plugin from Disk...**
5. Select the downloaded ZIP file
6. Restart the IDE

## 🔐 Authentication Setup

### EntraID SSO (Recommended)

1. Go to **Tools** → **RepoGate** → **Sign In with EntraID**
2. Enter your email address
3. Browser will open for Microsoft authentication
4. Complete sign-in in your browser
5. Return to IDE - you're authenticated!

**Features:**
- Single Sign-On with your organization's Microsoft account
- Automatic token refresh (no re-authentication needed)
- Enhanced security with OAuth2 PKCE flow
- Token rotation for long-lived sessions

### API Token (Legacy)

1. Go to **Tools** → **RepoGate** → **Sign In with API Token (Legacy)**
2. Enter your RepoGate API token
3. Token is securely stored in PasswordSafe

**Note:** API tokens don't expire but EntraID is recommended for better security.

## 🛠️ Configuration

### Settings Location
**Settings/Preferences** → **Tools** → **RepoGate**

### Available Settings
- **Enabled** - Enable/disable dependency monitoring
- **API URL** - RepoGate API endpoint (default: `https://app.repogate.io/api/v1`)
- **Poll Interval** - Status polling interval in milliseconds (default: 10000ms)
- **Include Dev Dependencies** - Monitor development dependencies (default: false)
- **Log Level** - Logging verbosity: error, warn, info, debug (default: error)

## 📋 Usage

### Tools Menu Actions

Access all RepoGate actions via **Tools** → **RepoGate**:

- **Sign In with EntraID** - Authenticate using Microsoft EntraID SSO
- **Sign In with API Token (Legacy)** - Authenticate using API token
- **Sign Out** - Clear authentication and stop monitoring
- **Show Account Info** - View current authentication status
- **Test Connection** - Verify connectivity to RepoGate API
- **Scan Now** - Manually trigger scan of all packages

### Automatic Monitoring

Once authenticated, RepoGate automatically:
1. Monitors dependency files for changes
2. Detects new dependencies and sends for validation
3. Polls for approval status
4. Shows notifications for status changes
5. Tracks dependency removals and version changes

### Approval Statuses

| Status | Icon | Description |
|--------|------|-------------|
| **Approved** | ✓ | Package is safe to use |
| **Denied** | ✗ | Package has security issues - should not be used |
| **Pending** | ⏳ | Awaiting security review |
| **Scanning** | 🔍 | Currently being scanned for vulnerabilities |
| **Not Found** | ❓ | Package not in database - submitted for review |

## 🐛 Troubleshooting

### Authentication Issues

**Problem:** "Authentication Required" notification
- **Solution:** Sign in using Tools → RepoGate → Sign In with EntraID (or API Token)

**Problem:** "Authentication Error - No valid token"
- **Solution:** Token may have expired. Sign out and sign in again.

**Problem:** EntraID authentication fails
- **Solution:** 
  - Check your email address is correct
  - Verify your organization uses EntraID SSO
  - Check browser allows popup windows
  - Try API Token authentication instead

### Connection Issues

**Problem:** "Waiting for RepoGate service to respond"
- **Solution:** 
  - Use Tools → RepoGate → Test Connection to verify connectivity
  - Check API URL in Settings → Tools → RepoGate
  - Verify RepoGate service is running
  - Check network/firewall settings

## 📝 Development

### Building from Source

```bash
# Clone repository
git clone https://github.com/SectorFlow/repogate-extension-jetbrains.git
cd repogate-extension-jetbrains

# Build plugin
./gradlew buildPlugin

# Output: build/distributions/RepoGate-2.0.0.zip
```

### Running in Development

```bash
# Run IDE with plugin
./gradlew runIde
```

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🔗 Links

- [RepoGate Website](https://repogate.io)
- [Documentation](https://repogate.io/docs)
- [Support](mailto:support@repogate.io)
- [VS Code Extension](https://github.com/SectorFlow/repogate-extention-vscode)

## 📊 Version History

### 2.0.0 (Current)
- EntraID OAuth2 authentication with PKCE
- Dual authentication modes (EntraID + API Token)
- Secure credential storage with PasswordSafe
- Automatic token refresh
- Multi-IDE support (all JetBrains IDEs)
- Enhanced API integration
- Git repository detection
- New user actions and improved UX

### 1.3.0
- Added scanning and not_found status support
- Improved status messages with icons
- Enhanced polling for all approval states

### 1.0.0
- Initial release
- Basic dependency monitoring
- API token authentication
- npm, Maven, Gradle support

---

**Made with ❤️ by the RepoGate Team**
