package io.repogate.plugin.actions;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import io.repogate.plugin.auth.AuthManager;
import io.repogate.plugin.service.InitialPackageScanner;
import org.jetbrains.annotations.NotNull;

/**
 * Action to manually scan all packages
 */
public class ScanNowAction extends AnAction {
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        AuthManager authManager = AuthManager.getInstance();
        
        if (!authManager.isAuthenticated()) {
            showNotification("No authentication configured", NotificationType.WARNING);
            return;
        }
        
        showNotification("Scanning packages...", NotificationType.INFORMATION);
        
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                InitialPackageScanner scanner = new InitialPackageScanner(project);
                // Force re-scan by resetting the flag
                project.getService(com.intellij.openapi.components.PersistentStateComponent.class);
                scanner.performInitialScanIfNeeded();
                
                showNotification("Package scan completed", NotificationType.INFORMATION);
            } catch (Exception ex) {
                showNotification(
                        "Scan failed: " + ex.getMessage(),
                        NotificationType.ERROR
                );
            }
        });
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        AuthManager authManager = AuthManager.getInstance();
        e.getPresentation().setEnabledAndVisible(
                e.getProject() != null && authManager.isAuthenticated()
        );
    }
    
    private void showNotification(String content, NotificationType type) {
        Notification notification = new Notification(
                "RepoGate Notifications",
                "RepoGate",
                content,
                type
        );
        Notifications.Bus.notify(notification);
    }
}
