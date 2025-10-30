package io.repogate.plugin.settings;

import com.intellij.ui.components.*;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class RepoGateSettingsComponent {
    private final JPanel mainPanel;
    private final JBCheckBox enabledCheckBox = new JBCheckBox("Enable RepoGate");
    private final JBTextField apiUrlField = new JBTextField();
    private final JBPasswordField apiTokenField = new JBPasswordField();

    public RepoGateSettingsComponent() {
        apiUrlField.setText("http://localhost:3000/api/v1");
        
        mainPanel = FormBuilder.createFormBuilder()
                .addComponent(enabledCheckBox, 1)
                .addLabeledComponent(new JBLabel("API URL:"), apiUrlField, 1, false)
                .addLabeledComponent(new JBLabel("API Token:"), apiTokenField, 1, false)
                .addComponentFillVertically(new JPanel(), 0)
                .addTooltip("Get your API token from RepoGate.io after signing up")
                .addSeparator()
                .addComponent(createInfoPanel(), 1)
                .getPanel();
    }

    private JPanel createInfoPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        JLabel infoLabel = new JLabel("<html><body style='width: 400px'>" +
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
                "<li>Get your API token from your dashboard</li>" +
                "<li>Enter the token above and click Apply</li>" +
                "</ol>" +
                "</body></html>");
        
        panel.add(infoLabel);
        return panel;
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public JComponent getPreferredFocusedComponent() {
        return apiTokenField;
    }

    @NotNull
    public String getApiToken() {
        return new String(apiTokenField.getPassword());
    }

    public void setApiToken(@NotNull String newText) {
        apiTokenField.setText(newText);
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
}
