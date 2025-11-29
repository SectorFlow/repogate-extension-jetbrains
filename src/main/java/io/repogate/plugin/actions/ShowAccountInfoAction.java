package io.repogate.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import io.repogate.plugin.auth.AuthManager;
import org.jetbrains.annotations.NotNull;

/**
 * Action to show account information
 */
public class ShowAccountInfoAction extends AnAction {
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        AuthManager authManager = AuthManager.getInstance();
        
        if (!authManager.isAuthenticated()) {
            Messages.showInfoMessage(
                    "You are not currently signed in.\n\n" +
                    "Use 'Sign In with EntraID' or 'Sign In with API Token' to authenticate.",
                    "Not Signed In"
            );
            return;
        }
        
        String status = authManager.getAuthStatus();
        String authMode = authManager.getAuthMode().name();
        
        StringBuilder info = new StringBuilder();
        info.append("Authentication Status: ").append(status).append("\n");
        info.append("Authentication Mode: ").append(authMode).append("\n");
        
        Messages.showInfoMessage(
                info.toString(),
                "RepoGate Account Information"
        );
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Always enabled
        e.getPresentation().setEnabledAndVisible(true);
    }
}
