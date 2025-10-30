package io.repogate.plugin.parser;

import io.repogate.plugin.model.DependencyInfo;
import java.util.List;

public interface DependencyParser {
    /**
     * Parse dependencies from file content
     * @param content The file content
     * @param previousContent The previous file content (for comparison)
     * @return List of newly added dependencies
     */
    List<DependencyInfo> parseNewDependencies(String content, String previousContent);
    
    /**
     * Check if this parser supports the given file
     * @param fileName The name of the file
     * @return true if this parser can handle the file
     */
    boolean supports(String fileName);
    
    /**
     * Get the package manager type
     * @return The package manager identifier (npm, maven, gradle)
     */
    String getPackageManager();
}
