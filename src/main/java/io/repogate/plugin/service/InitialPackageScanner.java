package io.repogate.plugin.service;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import io.repogate.plugin.api.RepoGateApiClient;
import io.repogate.plugin.model.DependencyInfo;
import io.repogate.plugin.parser.*;
import io.repogate.plugin.settings.RepoGateSettings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class InitialPackageScanner {
    private static final String SCAN_COMPLETED_KEY = "repogate.initialScanCompleted";
    private final Project project;
    private final List<DependencyParser> parsers;

    public InitialPackageScanner(Project project) {
        this.project = project;
        this.parsers = Arrays.asList(
                new NpmDependencyParser(),
                new MavenDependencyParser(),
                new GradleDependencyParser()
        );
    }

    public void performInitialScanIfNeeded() {
        // Check if scan was already completed for this project
        PropertiesComponent props = PropertiesComponent.getInstance(project);
        boolean scanCompleted = props.getBoolean(SCAN_COMPLETED_KEY, false);
        
        if (scanCompleted) {
            System.out.println("RepoGate: Initial scan already completed, skipping...");
            return;
        }

        RepoGateSettings settings = RepoGateSettings.getInstance();
        if (!settings.isEnabled() || settings.getApiToken() == null || settings.getApiToken().isEmpty()) {
            System.out.println("RepoGate: Skipping initial scan - extension not configured");
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                showNotification("RepoGate: Scanning existing packages...", NotificationType.INFORMATION);
                
                List<RepoGateApiClient.PackageInfo> allPackages = collectAllPackages();
                
                if (!allPackages.isEmpty()) {
                    queuePackages(allPackages);
                    showNotification(
                            String.format("RepoGate: Queued %d existing packages for review", allPackages.size()),
                            NotificationType.INFORMATION
                    );
                }
                
                // Mark scan as completed
                props.setValue(SCAN_COMPLETED_KEY, true);
                
            } catch (Exception e) {
                System.err.println("RepoGate: Error during initial scan: " + e.getMessage());
            }
        });
    }

    private List<RepoGateApiClient.PackageInfo> collectAllPackages() {
        List<RepoGateApiClient.PackageInfo> allPackages = new ArrayList<>();
        String projectName = project.getName();

        // Find all package.json files
        Collection<VirtualFile> packageJsonFiles = FilenameIndex.getVirtualFilesByName(
                "package.json",
                GlobalSearchScope.projectScope(project)
        );
        for (VirtualFile file : packageJsonFiles) {
            allPackages.addAll(parsePackages(file, new NpmDependencyParser(), projectName));
        }

        // Find all pom.xml files
        Collection<VirtualFile> pomFiles = FilenameIndex.getVirtualFilesByName(
                "pom.xml",
                GlobalSearchScope.projectScope(project)
        );
        for (VirtualFile file : pomFiles) {
            allPackages.addAll(parsePackages(file, new MavenDependencyParser(), projectName));
        }

        // Find all build.gradle files
        Collection<VirtualFile> gradleFiles = FilenameIndex.getVirtualFilesByName(
                "build.gradle",
                GlobalSearchScope.projectScope(project)
        );
        for (VirtualFile file : gradleFiles) {
            allPackages.addAll(parsePackages(file, new GradleDependencyParser(), projectName));
        }

        // Find all build.gradle.kts files
        Collection<VirtualFile> gradleKtsFiles = FilenameIndex.getVirtualFilesByName(
                "build.gradle.kts",
                GlobalSearchScope.projectScope(project)
        );
        for (VirtualFile file : gradleKtsFiles) {
            allPackages.addAll(parsePackages(file, new GradleDependencyParser(), projectName));
        }

        return allPackages;
    }

    private List<RepoGateApiClient.PackageInfo> parsePackages(VirtualFile file, DependencyParser parser, String projectName) {
        List<RepoGateApiClient.PackageInfo> packages = new ArrayList<>();
        
        try {
            String content = new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
            List<DependencyInfo> deps = parser.parseNewDependencies(content, "");
            
            for (DependencyInfo dep : deps) {
                packages.add(new RepoGateApiClient.PackageInfo(
                        dep.getPackageName(),
                        dep.getVersion(),
                        dep.getPackageManager(),
                        projectName
                ));
            }
        } catch (IOException e) {
            System.err.println("RepoGate: Error reading file " + file.getPath() + ": " + e.getMessage());
        }
        
        return packages;
    }

    private void queuePackages(List<RepoGateApiClient.PackageInfo> packages) {
        RepoGateSettings settings = RepoGateSettings.getInstance();
        String apiUrl = settings.getApiUrl();
        String apiToken = settings.getApiToken();

        if (apiUrl == null || apiUrl.isEmpty() || apiToken == null || apiToken.isEmpty()) {
            return;
        }

        try {
            RepoGateApiClient client = new RepoGateApiClient(apiUrl, apiToken);
            client.queuePackages(packages);
            System.out.println("RepoGate: Successfully queued " + packages.size() + " packages");
        } catch (Exception e) {
            System.err.println("RepoGate: Failed to queue packages: " + e.getMessage());
        }
    }

    private void showNotification(String content, NotificationType type) {
        ApplicationManager.getApplication().invokeLater(() -> {
            Notification notification = NotificationGroupManager.getInstance()
                    .getNotificationGroup("RepoGate Notifications")
                    .createNotification("RepoGate", content, type);
            Notifications.Bus.notify(notification, project);
        });
    }
}
