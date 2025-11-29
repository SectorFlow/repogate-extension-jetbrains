package io.repogate.plugin.settings;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class RepoGateConfigurable implements Configurable {
    private RepoGateSettingsComponent settingsComponent;

    @Override
    public String getDisplayName() {
        return "RepoGate";
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return settingsComponent.getPreferredFocusedComponent();
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        settingsComponent = new RepoGateSettingsComponent();
        return settingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        RepoGateSettings settings = RepoGateSettings.getInstance();
        return !settingsComponent.getApiUrl().equals(settings.getApiUrl()) ||
                settingsComponent.isEnabled() != settings.isEnabled();
    }

    @Override
    public void apply() {
        RepoGateSettings settings = RepoGateSettings.getInstance();
        settings.setApiUrl(settingsComponent.getApiUrl());
        settings.setEnabled(settingsComponent.isEnabled());
    }

    @Override
    public void reset() {
        RepoGateSettings settings = RepoGateSettings.getInstance();
        settingsComponent.setApiUrl(settings.getApiUrl());
        settingsComponent.setEnabled(settings.isEnabled());
    }

    @Override
    public void disposeUIResources() {
        settingsComponent = null;
    }
}
