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

                // Handle response based on new status values
                handleDependencyResponse(dependency, response);

            } catch (Exception e) {
                // Connection failed
                isConnected = false;
                String errorMsg = e.getMessage();
                
                if (errorMsg != null && (errorMsg.contains("Connection refused") || 
                        errorMsg.contains("Failed to connect") || 
                        errorMsg.contains("Network is unreachable"))) {
                    showNotification("⏳ RepoGate",
                            "Waiting for RepoGate service to start... Will retry automatically.",
                            NotificationType.WARNING);
                    
                    dependency.setStatus(DependencyInfo.ApprovalStatus.PENDING);
                    retryConnection(dependency);
                } else {
                    showNotification("RepoGate: Connection Error",
                            "Unable to connect - " + errorMsg,
                            NotificationType.ERROR);
                    dependency.setStatus(DependencyInfo.ApprovalStatus.ERROR);
                }
            }
        });
    }

    private void handleDependencyResponse(DependencyInfo dependency, RepoGateApiClient.DependencyResponse response) {
        String status = response.getStatus();
        if (status == null) {
            status = response.isApproved() ? "approved" : "pending";
        }

        switch (status.toLowerCase()) {
            case "approved":
                dependency.setStatus(DependencyInfo.ApprovalStatus.APPROVED);
                showNotification("✓ RepoGate",
                        String.format("%s - Package '%s' can be used.", 
                                response.getMessage(), dependency.getPackageName()),
                        NotificationType.INFORMATION);
                break;

            case "denied":
                dependency.setStatus(DependencyInfo.ApprovalStatus.DENIED);
                showNotification("✗ RepoGate",
                        String.format("%s - Package '%s' should not be used.", 
                                response.getMessage(), dependency.getPackageName()),
                        NotificationType.ERROR);
                break;

            case "pending":
                dependency.setStatus(DependencyInfo.ApprovalStatus.PENDING);
                showNotification("⏳ RepoGate",
                        response.getMessage(),
                        NotificationType.WARNING);
                // Start polling for approval status
                startPolling(dependency);
                break;

            case "scanning":
                dependency.setStatus(DependencyInfo.ApprovalStatus.SCANNING);
                showNotification("🔍 RepoGate",
                        response.getMessage(),
                        NotificationType.INFORMATION);
                // Start polling to check when scanning completes
                startPolling(dependency);
                break;

            case "not_found":
                dependency.setStatus(DependencyInfo.ApprovalStatus.NOT_FOUND);
                showNotification("❓ RepoGate",
                        response.getMessage(),
                        NotificationType.WARNING);
                // Start polling in case it gets added
                startPolling(dependency);
                break;

            default:
                // Fallback for unknown status
                dependency.setStatus(DependencyInfo.ApprovalStatus.PENDING);
                showNotification("⏳ RepoGate",
                        response.getMessage() != null ? response.getMessage() : "Package status unknown",
                        NotificationType.WARNING);
                startPolling(dependency);
                break;
        }
    }

    private void retryConnection(DependencyInfo dependency) {
        String key = dependency.getPackageName() + ":" + dependency.getPackageManager();
        
        // Cancel any existing polling
        ScheduledFuture<?> existingTask = pollingTasks.get(key);
        if (existingTask != null) {
            existingTask.cancel(false);
        }

        final int[] retryCount = {0};
        final int maxRetries = 30; // 5 minutes (30 * 10 seconds)

        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
            retryCount[0]++;
            
            if (retryCount[0] > maxRetries) {
                ScheduledFuture<?> currentTask = pollingTasks.remove(key);
                if (currentTask != null) {
                    currentTask.cancel(false);
                }
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
                ScheduledFuture<?> currentTask = pollingTasks.remove(key);
                if (currentTask != null) {
                    currentTask.cancel(false);
                }

                // Handle response
                handleDependencyResponse(dependency, response);

            } catch (Exception e) {
                // Still can't connect, will retry on next interval
                System.out.println(String.format("RepoGate: Retry %d/%d - still waiting for service...", retryCount[0], maxRetries));
            }
        }, 10, 10, TimeUnit.SECONDS);

        pollingTasks.put(key, task);
    }

    private void startPolling(DependencyInfo dependency) {
        String key = dependency.getPackageName() + ":" + dependency.getPackageManager();
        
        // Cancel any existing polling
        ScheduledFuture<?> existingTask = pollingTasks.get(key);
        if (existingTask != null) {
            existingTask.cancel(false);
        }

        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
            try {
                RepoGateSettings settings = RepoGateSettings.getInstance();
                RepoGateApiClient client = new RepoGateApiClient(settings.getApiUrl(), settings.getApiToken());
                
                RepoGateApiClient.DependencyResponse response = client.checkDependency(
                        dependency.getPackageName(),
                        dependency.getPackageManager()
                );

                String status = response.getStatus();
                if (status == null) {
                    status = response.isApproved() ? "approved" : "pending";
                }

                switch (status.toLowerCase()) {
                    case "approved":
                        dependency.setStatus(DependencyInfo.ApprovalStatus.APPROVED);
                        pendingDependencies.remove(key);
                        
                        ScheduledFuture<?> currentTask = pollingTasks.remove(key);
                        if (currentTask != null) {
                            currentTask.cancel(false);
                        }

                        showNotification("✓ RepoGate",
                                String.format("%s - Package '%s' can now be used.", 
                                        response.getMessage(), dependency.getPackageName()),
                                NotificationType.INFORMATION);
                        break;

                    case "denied":
                        dependency.setStatus(DependencyInfo.ApprovalStatus.DENIED);
                        pendingDependencies.remove(key);
                        
                        currentTask = pollingTasks.remove(key);
                        if (currentTask != null) {
                            currentTask.cancel(false);
                        }

                        ApplicationManager.getApplication().invokeLater(() -> {
                            int result = Messages.showYesNoDialog(
                                    project,
                                    String.format("%s\n\nPackage '%s' should not be used in production code.",
                                            response.getMessage(), dependency.getPackageName()),
                                    "✗ RepoGate: Package Denied",
                                    "I Understand",
                                    "Remove It",
                                    Messages.getErrorIcon()
                            );
                            
                            if (result == Messages.NO) {
                                showNotification("RepoGate",
                                        "Please manually remove the dependency from your configuration file.",
                                        NotificationType.INFORMATION);
                            }
                        });
                        break;

                    case "pending":
                        // Still pending, continue polling
                        dependency.setStatus(DependencyInfo.ApprovalStatus.PENDING);
                        break;

                    case "scanning":
                        // Still scanning, continue polling
                        dependency.setStatus(DependencyInfo.ApprovalStatus.SCANNING);
                        break;

                    case "not_found":
                        // Package not found, continue polling in case it gets added
                        dependency.setStatus(DependencyInfo.ApprovalStatus.NOT_FOUND);
                        break;
                }

            } catch (Exception e) {
                if (!isConnected) {
                    System.err.println("RepoGate: Lost connection during polling");
                }
            }
        }, 10, 10, TimeUnit.SECONDS);

        pollingTasks.put(key, task);
    }

    private void showNotification(String title, String content, NotificationType type) {
        ApplicationManager.getApplication().invokeLater(() -> {
            Notification notification = NotificationGroupManager.getInstance()
                    .getNotificationGroup("RepoGate Notifications")
                    .createNotification(title, content, type);
            Notifications.Bus.notify(notification, project);
        });
    }

    public void dispose() {
        // Cancel all polling tasks
        for (ScheduledFuture<?> task : pollingTasks.values()) {
            task.cancel(false);
        }
        pollingTasks.clear();
        pendingDependencies.clear();
        scheduler.shutdown();
    }
}
