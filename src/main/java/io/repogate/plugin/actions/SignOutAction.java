package io.repogate.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import io.repogate.plugin.auth.AuthManager;
import org.jetbrains.annotations.NotNull;

/**
 * Action to sign out
 */
public class SignOutAction extends AnAction {
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        AuthManager authManager = AuthManager.getInstance();
        
        if (!authManager.isAuthenticated()) {
            Messages.showInfoMessage(
                    "You are not currently signed in.",
                    "Not Signed In"
            );
            return;
        }
        
        int result = Messages.showYesNoDialog(
                "Are you sure you want to sign out? The extension will stop monitoring dependencies.",
                "Confirm Sign Out",
                Messages.getQuestionIcon()
        );
        
        if (result == Messages.YES) {
            authManager.signOut();
            Messages.showInfoMessage(
                    "You have been signed out successfully.",
                    "Signed Out"
            );
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        AuthManager authManager = AuthManager.getInstance();
        e.getPresentation().setEnabledAndVisible(authManager.isAuthenticated());
    }
}
