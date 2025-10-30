package io.repogate.plugin.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
        name = "io.repogate.plugin.settings.RepoGateSettings",
        storages = @Storage("RepoGateSettings.xml")
)
public class RepoGateSettings implements PersistentStateComponent<RepoGateSettings> {
    private String apiToken = "";
    private String apiUrl = "http://localhost:3000/api/v1";
    private boolean enabled = true;

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

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
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
