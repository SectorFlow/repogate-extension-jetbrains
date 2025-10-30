package io.repogate.plugin.parser;

import io.repogate.plugin.model.DependencyInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MavenDependencyParser implements DependencyParser {
    private static final Pattern DEPENDENCY_PATTERN = Pattern.compile(
            "<dependency>\\s*<groupId>([^<]+)</groupId>\\s*<artifactId>([^<]+)</artifactId>\\s*(?:<version>([^<]+)</version>)?",
            Pattern.DOTALL
    );

    @Override
    public List<DependencyInfo> parseNewDependencies(String content, String previousContent) {
        List<DependencyInfo> newDependencies = new ArrayList<>();
        
        try {
            Set<String> previousDeps = extractDependencies(previousContent);
            Set<String> currentDeps = extractDependencies(content);
            
            // Find new dependencies
            Matcher matcher = DEPENDENCY_PATTERN.matcher(content);
            while (matcher.find()) {
                String groupId = matcher.group(1).trim();
                String artifactId = matcher.group(2).trim();
                String version = matcher.group(3) != null ? matcher.group(3).trim() : "";
                String fullName = groupId + ":" + artifactId;
                
                if (!previousDeps.contains(fullName) && currentDeps.contains(fullName)) {
                    newDependencies.add(new DependencyInfo(fullName, "maven", version));
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing Maven dependencies: " + e.getMessage());
        }
        
        return newDependencies;
    }

    private Set<String> extractDependencies(String content) {
        Set<String> deps = new HashSet<>();
        if (content == null || content.trim().isEmpty()) {
            return deps;
        }
        
        try {
            Matcher matcher = DEPENDENCY_PATTERN.matcher(content);
            while (matcher.find()) {
                String groupId = matcher.group(1).trim();
                String artifactId = matcher.group(2).trim();
                deps.add(groupId + ":" + artifactId);
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        
        return deps;
    }

    @Override
    public boolean supports(String fileName) {
        return "pom.xml".equals(fileName);
    }

    @Override
    public String getPackageManager() {
        return "maven";
    }
}
