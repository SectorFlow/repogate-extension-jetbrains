package io.repogate.plugin.utils;

import com.intellij.openapi.project.Project;

import java.io.File;

/**
 * Utility to detect if project is a Git repository
 */
public class GitDetector {
    
    /**
     * Check if the project is linked to a Git repository
     * Simple implementation: checks for .git directory
     */
    public static boolean isGitRepository(Project project) {
        if (project == null) {
            return false;
        }
        
        // Check for .git directory
        String basePath = project.getBasePath();
        if (basePath != null) {
            File gitDir = new File(basePath, ".git");
            return gitDir.exists() && gitDir.isDirectory();
        }
        
        return false;
    }
}
