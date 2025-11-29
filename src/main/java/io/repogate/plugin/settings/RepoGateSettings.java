package io.repogate.plugin.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import io.repogate.plugin.auth.AuthMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
        name = "io.repogate.plugin.settings.RepoGateSettings",
        storages = @Storage("RepoGateSettings.xml")
)
public class RepoGateSettings implements PersistentStateComponent<RepoGateSettings> {
    // Removed apiToken - now stored securely in PasswordSafe
    private String apiUrl = "https://app.repogate.io/api/v1";
    private boolean enabled = true;
    private String authMode = "UNAUTHENTICATED"; // ENTRA_SSO, LOCAL_TOKEN, or UNAUTHENTICATED
    private int pollIntervalMs = 10000;
    private boolean includeDevDependencies = false;
    private String logLevel = "error";

    public static RepoGateSettings getInstance() {
        return ApplicationManager.getApplication().getService(RepoGateSettings.class);
    }

    @Override
    public RepoGateSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull RepoGateSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public AuthMode getAuthMode() {
        try {
            return AuthMode.valueOf(authMode);
        } catch (Exception e) {
            return AuthMode.UNAUTHENTICATED;
        }
    }

    public void setAuthMode(AuthMode mode) {
        this.authMode = mode.name();
    }

    public int getPollIntervalMs() {
        return Math.max(pollIntervalMs, 3000); // Minimum 3 seconds
    }

    public void setPollIntervalMs(int pollIntervalMs) {
        this.pollIntervalMs = Math.max(pollIntervalMs, 3000);
    }

    public boolean isIncludeDevDependencies() {
        return includeDevDependencies;
    }

    public void setIncludeDevDependencies(boolean includeDevDependencies) {
        this.includeDevDependencies = includeDevDependencies;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
