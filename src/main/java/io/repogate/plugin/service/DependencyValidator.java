package io.repogate.plugin.service;

import com.intellij.notification.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.project.ProjectManager;
import io.repogate.plugin.api.RepoGateApiClient;
import io.repogate.plugin.model.DependencyInfo;
import io.repogate.plugin.settings.RepoGateSettings;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DependencyValidator {
    private final Project project;
    private final ConcurrentHashMap<String, DependencyInfo> pendingDependencies = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public DependencyValidator(Project project) {
        this.project = project;
    }

    /**
     * Validate a newly detected dependency
     */
    public void validateDependency(DependencyInfo dependency) {
        RepoGateSettings settings = RepoGateSettings.getInstance();
        
        if (!settings.isEnabled()) {
            // RepoGate is disabled, allow the dependency
            return;
        }

        String apiToken = settings.getApiToken();
        if (apiToken == null || apiToken.trim().isEmpty()) {
            showNotification("RepoGate: API Token Required",
                    "Please configure your RepoGate API token in Settings > Tools > RepoGate",
                    NotificationType.WARNING);
            return;
        }

        String key = dependency.getPackageName() + ":" + dependency.getPackageManager();
        pendingDependencies.put(key, dependency);

        // Request validation
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                RepoGateApiClient client = new RepoGateApiClient(settings.getApiUrl(), apiToken);
                RepoGateApiClient.DependencyResponse response = client.requestDependency(
                        dependency.getPackageName(),
                        dependency.getPackageManager()
                );

                showNotification("RepoGate: Dependency Validation",
                        response.getMessage(),
                        NotificationType.INFORMATION);

                // Start polling for approval status
                startPolling(dependency);

            } catch (Exception e) {
                showNotification("RepoGate: Validation Error",
                        "Failed to validate dependency: " + e.getMessage(),
                        NotificationType.ERROR);
                dependency.setStatus(DependencyInfo.ApprovalStatus.ERROR);
            }
        });
    }

    /**
     * Start polling for dependency approval status
     */
    private void startPolling(DependencyInfo dependency) {
        String key = dependency.getPackageName() + ":" + dependency.getPackageManager();
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                RepoGateSettings settings = RepoGateSettings.getInstance();
                RepoGateApiClient client = new RepoGateApiClient(settings.getApiUrl(), settings.getApiToken());
                
                RepoGateApiClient.DependencyResponse response = client.checkDependency(
                        dependency.getPackageName(),
                        dependency.getPackageManager()
                );

                if (response.isApproved()) {
                    dependency.setStatus(DependencyInfo.ApprovalStatus.APPROVED);
                    pendingDependencies.remove(key);
                    
                    showNotification("RepoGate: Dependency Approved",
                            String.format("Package '%s' has been approved and can be used.",
                                    dependency.getPackageName()),
                            NotificationType.INFORMATION);
                    
                    // Stop polling
                    return;
                } else if ("denied".equalsIgnoreCase(response.getStatus())) {
                    dependency.setStatus(DependencyInfo.ApprovalStatus.DENIED);
                    pendingDependencies.remove(key);
                    
                    // Show denial notification with option to remove
                    ApplicationManager.getApplication().invokeLater(() -> {
                        int result = Messages.showYesNoDialog(
                                project,
                                String.format("Package '%s' has been DENIED by your security team.\n\n%s\n\nWould you like to remove it from your project?",
                                        dependency.getPackageName(),
                                        response.getMessage()),
                                "RepoGate: Dependency Denied",
                                "Remove Dependency",
                                "Keep Anyway",
                                Messages.getWarningIcon()
                        );
                        
                        if (result == Messages.YES) {
                            // TODO: Implement quick removal
                            showNotification("RepoGate: Manual Removal Required",
                                    "Please manually remove the dependency from your configuration file.",
                                    NotificationType.WARNING);
                        }
                    });
                    
                    // Stop polling
                    return;
                }
                
            } catch (Exception e) {
                System.err.println("Error checking dependency status: " + e.getMessage());
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    /**
     * Show a notification to the user
     */
    private void showNotification(String title, String content, NotificationType type) {
        ApplicationManager.getApplication().invokeLater(() -> {
            Notification notification = NotificationGroupManager.getInstance()
                    .getNotificationGroup("RepoGate Notifications")
                    .createNotification(title, content, type);
            Notifications.Bus.notify(notification, project);
        });
    }

    /**
     * Shutdown the validator
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}
