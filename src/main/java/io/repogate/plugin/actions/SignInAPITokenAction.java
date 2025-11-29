package io.repogate.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import io.repogate.plugin.auth.AuthManager;
import org.jetbrains.annotations.NotNull;

/**
 * Action to sign in with API Token
 */
public class SignInAPITokenAction extends AnAction {
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        AuthManager authManager = AuthManager.getInstance();
        
        if (authManager.isAuthenticated()) {
            int result = Messages.showYesNoDialog(
                    "You are already signed in. Do you want to sign out and sign in again?",
                    "Already Signed In",
                    Messages.getQuestionIcon()
            );
            
            if (result == Messages.YES) {
                authManager.signOut();
            } else {
                return;
            }
        }
        
        boolean success = authManager.signInWithAPIToken();
        
        if (!success) {
            Messages.showErrorDialog(
                    "Failed to sign in with API Token. Please try again.",
                    "Sign-In Failed"
            );
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Always enabled
        e.getPresentation().setEnabledAndVisible(true);
    }
}
