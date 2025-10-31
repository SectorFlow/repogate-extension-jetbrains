package io.repogate.plugin.listeners;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import io.repogate.plugin.model.DependencyInfo;
import io.repogate.plugin.parser.DependencyParser;
import io.repogate.plugin.parser.GradleDependencyParser;
import io.repogate.plugin.parser.MavenDependencyParser;
import io.repogate.plugin.parser.NpmDependencyParser;
import io.repogate.plugin.service.DependencyValidator;
import io.repogate.plugin.service.InventoryReporter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DependencyFileListener implements BulkFileListener {
    private final List<DependencyParser> parsers = Arrays.asList(
            new NpmDependencyParser(),
            new MavenDependencyParser(),
            new GradleDependencyParser()
    );
    
    private final Map<String, String> fileContentsCache = new ConcurrentHashMap<>();
    private final Map<Project, DependencyValidator> validators = new ConcurrentHashMap<>();
    private final Map<Project, InventoryReporter> inventoryReporters = new ConcurrentHashMap<>();
    private final Set<Project> inventoryReported = ConcurrentHashMap.newKeySet();

    @Override
    public void after(@NotNull List<? extends VFileEvent> events) {
        for (VFileEvent event : events) {
            if (event instanceof VFileContentChangeEvent) {
                VFileContentChangeEvent changeEvent = (VFileContentChangeEvent) event;
                VirtualFile file = changeEvent.getFile();
                
                // Check if this is a dependency file we care about
                String fileName = file.getName();
                DependencyParser parser = findParser(fileName);
                
                if (parser != null) {
                    handleDependencyFileChange(file, parser);
                }
            }
        }
    }

    private DependencyParser findParser(String fileName) {
        for (DependencyParser parser : parsers) {
            if (parser.supports(fileName)) {
                return parser;
            }
        }
        return null;
    }

    private void handleDependencyFileChange(VirtualFile file, DependencyParser parser) {
        try {
            String filePath = file.getPath();
            String currentContent = new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
            String previousContent = fileContentsCache.getOrDefault(filePath, "");
            
            // Parse new dependencies
            List<DependencyInfo> newDependencies = parser.parseNewDependencies(currentContent, previousContent);
            
            if (!newDependencies.isEmpty()) {
                // Find the project for this file
                Project project = findProjectForFile(file);
                if (project != null) {
                    // Report inventory on first file change
                    if (!inventoryReported.contains(project)) {
                        InventoryReporter reporter = inventoryReporters.computeIfAbsent(
                                project,
                                InventoryReporter::new
                        );
                        reporter.reportInventoryIfNeeded();
                        inventoryReported.add(project);
                    }
                    
                    DependencyValidator validator = validators.computeIfAbsent(
                            project,
                            DependencyValidator::new
                    );
                    
                    // Validate each new dependency
                    for (DependencyInfo dependency : newDependencies) {
                        validator.validateDependency(dependency);
                    }
                }
            }
            
            // Update cache
            fileContentsCache.put(filePath, currentContent);
            
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }

    private Project findProjectForFile(VirtualFile file) {
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        for (Project project : openProjects) {
            if (project.getBasePath() != null && file.getPath().startsWith(project.getBasePath())) {
                return project;
            }
        }
        return null;
    }
}
