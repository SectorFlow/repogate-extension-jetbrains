package io.repogate.plugin.service;

import com.intellij.notification.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import io.repogate.plugin.api.RepoGateApiClient;
import io.repogate.plugin.model.DependencyInfo;
import io.repogate.plugin.settings.RepoGateSettings;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DependencyValidator {
    private final Project project;
    private final ConcurrentHashMap<String, DependencyInfo> pendingDependencies = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ScheduledFuture<?>> pollingTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private boolean isConnected = false;

    public DependencyValidator(Project project) {
        this.project = project;
    }

    /**
     * Validate a newly detected dependency
     */
    public void validateDependency(DependencyInfo dependency) {
        RepoGateSettings settings = RepoGateSettings.getInstance();
        
        if (!settings.isEnabled()) {
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

        // Show waiting message
        showNotification("⏳ RepoGate",
                "Waiting for RepoGate service to respond...",
                NotificationType.INFORMATION);

        // Request validation
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                RepoGateApiClient client = new RepoGateApiClient(settings.getApiUrl(), apiToken);
                String projectName = project.getName();
                RepoGateApiClient.DependencyResponse response = client.requestDependency(
                        dependency.getPackageName(),
                        dependency.getPackageManager(),
                        dependency.getVersion(),
                        projectName
                );

                // Connection successful!
                if (!isConnected) {
                    isConnected = true;
                    showNotification("✓ RepoGate",
                            "Connected successfully to RepoGate service",
                            NotificationType.INFORMATION);
                }

                // Handle response
                if (response.isApproved()) {
                    dependency.setStatus(DependencyInfo.ApprovalStatus.APPROVED);
                    showNotification("✓ RepoGate",
                            String.format("Package '%s' is already APPROVED", dependency.getPackageName()),
                            NotificationType.INFORMATION);
                } else if ("denied".equalsIgnoreCase(response.getStatus())) {
                    dependency.setStatus(DependencyInfo.ApprovalStatus.DENIED);
                    showNotification("✗ RepoGate",
                            String.format("Package '%s' is DENIED - %s", 
                                    dependency.getPackageName(), response.getMessage()),
                            NotificationType.ERROR);
                } else {
                    dependency.setStatus(DependencyInfo.ApprovalStatus.PENDING);
                    showNotification("⏳ RepoGate",
                            response.getMessage(),
                            NotificationType.WARNING);
                    // Start polling for approval status
                    startPolling(dependency);
                }

            } catch (Exception e) {
                // Connection failed
                isConnected = false;
                String errorMsg = e.getMessage();
                
                if (errorMsg != null && (errorMsg.contains("Connection refused") || 
                                        errorMsg.contains("Failed to connect") ||
                                        errorMsg.contains("Network") ||
                                        errorMsg.contains("timeout"))) {
                    showNotification("⏳ RepoGate",
                            "Waiting for RepoGate service to start... Will retry automatically.",
                            NotificationType.WARNING);
                    
                    dependency.setStatus(DependencyInfo.ApprovalStatus.PENDING);
                    // Start retry logic
                    startRetrying(dependency);
                } else {
                    showNotification("RepoGate: Connection Error",
                            "Unable to connect - " + errorMsg,
                            NotificationType.ERROR);
                    dependency.setStatus(DependencyInfo.ApprovalStatus.ERROR);
                }
            }
        });
    }

    /**
     * Retry connection to RepoGate service
     */
    private void startRetrying(DependencyInfo dependency) {
        String key = dependency.getPackageName() + ":" + dependency.getPackageManager();
        
        // Cancel any existing retry task
        ScheduledFuture<?> existingTask = pollingTasks.get(key);
        if (existingTask != null) {
            existingTask.cancel(false);
        }

        final int[] retryCount = {0};
        final int maxRetries = 30; // 5 minutes (30 * 10 seconds)

        ScheduledFuture<?> retryTask = scheduler.scheduleAtFixedRate(() -> {
            retryCount[0]++;
            
            if (retryCount[0] > maxRetries) {
                pollingTasks.remove(key);
                showNotification("RepoGate: Connection Timeout",
                        String.format("Could not connect to service after %d attempts. Please check if the service is running.", maxRetries),
                        NotificationType.WARNING);
                return;
            }

            try {
                RepoGateSettings settings = RepoGateSettings.getInstance();
                RepoGateApiClient client = new RepoGateApiClient(settings.getApiUrl(), settings.getApiToken());
                String projectName = project.getName();
                
                RepoGateApiClient.DependencyResponse response = client.requestDependency(
                        dependency.getPackageName(),
                        dependency.getPackageManager(),
                        dependency.getVersion(),
                        projectName
                );

                // Connection successful!
                if (!isConnected) {
                    isConnected = true;
                    showNotification("✓ RepoGate",
                            "Connected successfully to RepoGate service",
                            NotificationType.INFORMATION);
                }

                // Stop retrying
                ScheduledFuture<?> task = pollingTasks.remove(key);
                if (task != null) {
                    task.cancel(false);
                }

                // Handle response
                if (response.isApproved()) {
                    dependency.setStatus(DependencyInfo.ApprovalStatus.APPROVED);
                    showNotification("✓ RepoGate",
                            String.format("Package '%s' is APPROVED", dependency.getPackageName()),
                            NotificationType.INFORMATION);
                } else if ("denied".equalsIgnoreCase(response.getStatus())) {
                    dependency.setStatus(DependencyInfo.ApprovalStatus.DENIED);
                    showNotification("✗ RepoGate",
                            String.format("Package '%s' is DENIED", dependency.getPackageName()),
                            NotificationType.ERROR);
                } else {
                    dependency.setStatus(DependencyInfo.ApprovalStatus.PENDING);
                    // Start normal polling
                    startPolling(dependency);
                }

            } catch (Exception e) {
                // Still can't connect, will retry on next interval
                System.out.println("RepoGate: Retry " + retryCount[0] + "/" + maxRetries + " - still waiting for service...");
            }
        }, 10, 10, TimeUnit.SECONDS);

        pollingTasks.put(key, retryTask);
    }

    /**
     * Start polling for dependency approval status
     */
    private void startPolling(DependencyInfo dependency) {
        String key = dependency.getPackageName() + ":" + dependency.getPackageManager();
        
        // Cancel any existing polling task
        ScheduledFuture<?> existingTask = pollingTasks.get(key);
        if (existingTask != null) {
            existingTask.cancel(false);
        }

        ScheduledFuture<?> pollingTask = scheduler.scheduleAtFixedRate(() -> {
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
                    
                    // Stop polling
                    ScheduledFuture<?> task = pollingTasks.remove(key);
                    if (task != null) {
                        task.cancel(false);
                    }
                    
                    showNotification("✓ RepoGate: Dependency Approved",
                            String.format("Package '%s' has been APPROVED and can be used.",
                                    dependency.getPackageName()),
                            NotificationType.INFORMATION);
                    
                } else if ("denied".equalsIgnoreCase(response.getStatus())) {
                    dependency.setStatus(DependencyInfo.ApprovalStatus.DENIED);
                    pendingDependencies.remove(key);
                    
                    // Stop polling
                    ScheduledFuture<?> task = pollingTasks.remove(key);
                    if (task != null) {
                        task.cancel(false);
                    }
                    
                    // Show denial notification with option to remove
                    ApplicationManager.getApplication().invokeLater(() -> {
                        int result = Messages.showYesNoDialog(
                                project,
                                String.format("Package '%s' has been DENIED by your security team.\n\n%s\n\nThis package should not be used in production code.\n\nWould you like to remove it from your project?",
                                        dependency.getPackageName(),
                                        response.getMessage()),
                                "✗ RepoGate: Dependency Denied",
                                "Remove It",
                                "I Understand",
                                Messages.getWarningIcon()
                        );
                        
                        if (result == Messages.YES) {
                            showNotification("RepoGate: Manual Removal Required",
                                    "Please manually remove the dependency from your configuration file.",
                                    NotificationType.WARNING);
                        }
                    });
                }
                
            } catch (Exception e) {
                // If connection lost during polling
                if (!isConnected) {
                    System.err.println("RepoGate: Lost connection during polling");
                }
            }
        }, 10, 10, TimeUnit.SECONDS);

        pollingTasks.put(key, pollingTask);
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
        // Cancel all polling tasks
        for (ScheduledFuture<?> task : pollingTasks.values()) {
            task.cancel(false);
        }
        pollingTasks.clear();
        
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
