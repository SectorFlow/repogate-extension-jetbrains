package io.repogate.plugin.service;

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

public class InventoryReporter {
    private final Project project;
    private final List<DependencyParser> parsers;
    private boolean inventoryReported = false;

    public InventoryReporter(Project project) {
        this.project = project;
        this.parsers = Arrays.asList(
                new NpmDependencyParser(),
                new MavenDependencyParser(),
                new GradleDependencyParser()
        );
    }

    public void reportInventoryIfNeeded() {
        if (inventoryReported) {
            return;
        }

        RepoGateSettings settings = RepoGateSettings.getInstance();
        if (!settings.isEnabled() || settings.getApiToken() == null || settings.getApiToken().isEmpty()) {
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                List<DependencyInfo> allDependencies = collectAllDependencies();
                if (!allDependencies.isEmpty()) {
                    reportInventory(allDependencies);
                    inventoryReported = true;
                }
            } catch (Exception e) {
                System.err.println("RepoGate: Error collecting inventory: " + e.getMessage());
            }
        });
    }

    private List<DependencyInfo> collectAllDependencies() {
        List<DependencyInfo> allDeps = new ArrayList<>();

        // Find all package.json files
        Collection<VirtualFile> packageJsonFiles = FilenameIndex.getVirtualFilesByName(
                "package.json",
                GlobalSearchScope.projectScope(project)
        );
        for (VirtualFile file : packageJsonFiles) {
            allDeps.addAll(parseDependencies(file, new NpmDependencyParser()));
        }

        // Find all pom.xml files
        Collection<VirtualFile> pomFiles = FilenameIndex.getVirtualFilesByName(
                "pom.xml",
                GlobalSearchScope.projectScope(project)
        );
        for (VirtualFile file : pomFiles) {
            allDeps.addAll(parseDependencies(file, new MavenDependencyParser()));
        }

        // Find all build.gradle files
        Collection<VirtualFile> gradleFiles = FilenameIndex.getVirtualFilesByName(
                "build.gradle",
                GlobalSearchScope.projectScope(project)
        );
        for (VirtualFile file : gradleFiles) {
            allDeps.addAll(parseDependencies(file, new GradleDependencyParser()));
        }

        // Find all build.gradle.kts files
        Collection<VirtualFile> gradleKtsFiles = FilenameIndex.getVirtualFilesByName(
                "build.gradle.kts",
                GlobalSearchScope.projectScope(project)
        );
        for (VirtualFile file : gradleKtsFiles) {
            allDeps.addAll(parseDependencies(file, new GradleDependencyParser()));
        }

        return allDeps;
    }

    private List<DependencyInfo> parseDependencies(VirtualFile file, DependencyParser parser) {
        try {
            String content = new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
            return parser.parseNewDependencies(content, "");
        } catch (IOException e) {
            System.err.println("RepoGate: Error reading file " + file.getPath() + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private void reportInventory(List<DependencyInfo> dependencies) {
        RepoGateSettings settings = RepoGateSettings.getInstance();
        String apiUrl = settings.getApiUrl();
        String apiToken = settings.getApiToken();

        if (apiUrl == null || apiUrl.isEmpty() || apiToken == null || apiToken.isEmpty()) {
            return;
        }

        try {
            RepoGateApiClient client = new RepoGateApiClient(apiUrl, apiToken);
            
            Map<String, String> developerInfo = new HashMap<>();
            developerInfo.put("username", System.getProperty("user.name"));
            developerInfo.put("hostname", java.net.InetAddress.getLocalHost().getHostName());
            developerInfo.put("os", System.getProperty("os.name"));
            developerInfo.put("projectName", project.getName());
            developerInfo.put("ideVersion", com.intellij.openapi.application.ApplicationInfo.getInstance().getFullVersion());
            
            client.reportInventory(dependencies, developerInfo);
            System.out.println("RepoGate: Reported inventory of " + dependencies.size() + " dependencies");
        } catch (Exception e) {
            System.err.println("RepoGate: Failed to report inventory: " + e.getMessage());
        }
    }
}
