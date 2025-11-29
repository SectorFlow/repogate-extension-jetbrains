package io.repogate.plugin.actions;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import io.repogate.plugin.auth.AuthManager;
import io.repogate.plugin.settings.RepoGateSettings;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * Action to test connection to RepoGate API
 */
public class TestConnectionAction extends AnAction {
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        AuthManager authManager = AuthManager.getInstance();
        
        if (!authManager.isAuthenticated()) {
            showNotification("No authentication configured", NotificationType.WARNING);
            return;
        }
        
        showNotification("Testing connection...", NotificationType.INFORMATION);
        
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                RepoGateSettings settings = RepoGateSettings.getInstance();
                String apiUrl = settings.getApiUrl();
                String token = authManager.getToken();
                
                if (token == null) {
                    showNotification("No authentication token available", NotificationType.ERROR);
                    return;
                }
                
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(10, TimeUnit.SECONDS)
                        .build();
                
                Request request = new Request.Builder()
                        .url(apiUrl + "/health")
                        .get()
                        .addHeader("Authorization", "Bearer " + token)
                        .build();
                
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        showNotification(
                                "Connection successful!\n\nAPI URL: " + apiUrl + "\nStatus: Connected",
                                NotificationType.INFORMATION
                        );
                    } else {
                        showNotification(
                                "Connection failed!\n\nStatus code: " + response.code() +
                                "\n\nPlease verify:\n1. RepoGate service is running\n2. API URL is correct\n3. Authentication is valid",
                                NotificationType.ERROR
                        );
                    }
                }
                
            } catch (Exception ex) {
                showNotification(
                        "Connection test failed: " + ex.getMessage(),
                        NotificationType.ERROR
                );
            }
        });
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        AuthManager authManager = AuthManager.getInstance();
        e.getPresentation().setEnabledAndVisible(authManager.isAuthenticated());
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
