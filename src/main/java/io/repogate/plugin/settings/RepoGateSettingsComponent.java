package io.repogate.plugin.settings;

import com.intellij.ui.components.*;
import com.intellij.util.ui.FormBuilder;
import io.repogate.plugin.auth.AuthManager;
import io.repogate.plugin.model.UserInfo;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class RepoGateSettingsComponent {
    private final JPanel mainPanel;
    private final JBCheckBox enabledCheckBox = new JBCheckBox("Enable RepoGate");
    private final JBTextField apiUrlField = new JBTextField();
    private final JLabel authStatusLabel = new JLabel();
    private final JButton signInEntraIdButton = new JButton("Sign In with EntraID");
    private final JButton signInApiTokenButton = new JButton("Sign In with API Token");
    private final JButton signOutButton = new JButton("Sign Out");

    public RepoGateSettingsComponent() {
        apiUrlField.setText("https://app.repogate.io/api/v1");
        
        // Configure buttons
        signInEntraIdButton.addActionListener(e -> {
            io.repogate.plugin.actions.SignInEntraIDAction action = new io.repogate.plugin.actions.SignInEntraIDAction();
            action.actionPerformed(null);
            updateAuthStatus();
        });
        
        signInApiTokenButton.addActionListener(e -> {
            io.repogate.plugin.actions.SignInAPITokenAction action = new io.repogate.plugin.actions.SignInAPITokenAction();
            action.actionPerformed(null);
            updateAuthStatus();
        });
        
        signOutButton.addActionListener(e -> {
            io.repogate.plugin.actions.SignOutAction action = new io.repogate.plugin.actions.SignOutAction();
            action.actionPerformed(null);
            updateAuthStatus();
        });
        
        // Update initial auth status
        updateAuthStatus();
        
        // Create authentication panel
        JPanel authPanel = new JPanel();
        authPanel.setLayout(new BoxLayout(authPanel, BoxLayout.Y_AXIS));
        
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(authStatusLabel);
        authPanel.add(statusPanel);
        
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonsPanel.add(signInEntraIdButton);
        buttonsPanel.add(signInApiTokenButton);
        buttonsPanel.add(signOutButton);
        authPanel.add(buttonsPanel);
        
        mainPanel = FormBuilder.createFormBuilder()
                .addComponent(enabledCheckBox, 1)
                .addLabeledComponent(new JBLabel("API URL:"), apiUrlField, 1, false)
                .addSeparator()
                .addComponent(new JBLabel("<html><b>Authentication</b></html>"), 1)
                .addComponent(authPanel, 1)
                .addComponentFillVertically(new JPanel(), 0)
                .addSeparator()
                .addComponent(createInfoPanel(), 1)
                .getPanel();
    }
    
    private void updateAuthStatus() {
        AuthManager authManager = AuthManager.getInstance();
        
        if (authManager.isAuthenticated()) {
            UserInfo userInfo = authManager.getUserInfo();
            String authType = authManager.getAuthType();
            
            if (userInfo != null && userInfo.getEmail() != null) {
                authStatusLabel.setText("<html><span style='color: green;'>✓ Authenticated</span> - " + 
                                       userInfo.getEmail() + " (" + authType + ")</html>");
            } else {
                authStatusLabel.setText("<html><span style='color: green;'>✓ Authenticated</span> (" + authType + ")</html>");
            }
            
            signInEntraIdButton.setEnabled(false);
            signInApiTokenButton.setEnabled(false);
            signOutButton.setEnabled(true);
        } else {
            authStatusLabel.setText("<html><span style='color: gray;'>Not authenticated</span></html>");
            signInEntraIdButton.setEnabled(true);
            signInApiTokenButton.setEnabled(true);
            signOutButton.setEnabled(false);
        }
    }

    private JPanel createInfoPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        JLabel infoLabel = new JLabel("<html><body style='width: 500px'>" +
                "<h3>About RepoGate</h3>" +
                "<p>RepoGate monitors your project dependencies and validates them against security vulnerabilities.</p>" +
                "<p><b>Features:</b></p>" +
                "<ul>" +
                "<li>Monitors package.json (npm), pom.xml (Maven), and build.gradle (Gradle)</li>" +
                "<li>Validates dependencies via RepoGate API</li>" +
                "<li>Blocks installation of unapproved dependencies</li>" +
                "<li>Provides quick removal of denied dependencies</li>" +
                "</ul>" +
                "<p><b>Setup:</b></p>" +
                "<ol>" +
                "<li>Sign up at <a href='https://repogate.io'>RepoGate.io</a></li>" +
                "<li>Click <b>Sign In with EntraID</b> (recommended) or <b>Sign In with API Token</b> (legacy)</li>" +
                "<li>Complete authentication in your browser</li>" +
                "</ol>" +
                "</body></html>");
        
        panel.add(infoLabel);
        return panel;
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public JComponent getPreferredFocusedComponent() {
        return apiUrlField;
    }

    @NotNull
    public String getApiUrl() {
        return apiUrlField.getText();
    }

    public void setApiUrl(@NotNull String newText) {
        apiUrlField.setText(newText);
    }

    public boolean isEnabled() {
        return enabledCheckBox.isSelected();
    }

    public void setEnabled(boolean enabled) {
        enabledCheckBox.setSelected(enabled);
    }
    
    // Refresh auth status when settings panel is opened
    public void reset() {
        updateAuthStatus();
    }
}
