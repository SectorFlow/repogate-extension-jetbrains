package io.repogate.plugin.parser;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.repogate.plugin.model.DependencyInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NpmDependencyParser implements DependencyParser {
    private final Gson gson = new Gson();

    @Override
    public List<DependencyInfo> parseNewDependencies(String content, String previousContent) {
        List<DependencyInfo> newDependencies = new ArrayList<>();
        
        try {
            Set<String> previousDeps = extractDependencies(previousContent);
            Set<String> currentDeps = extractDependencies(content);
            
            JsonObject currentJson = gson.fromJson(content, JsonObject.class);
            JsonObject dependencies = currentJson.has("dependencies") ? 
                    currentJson.getAsJsonObject("dependencies") : new JsonObject();
            JsonObject devDependencies = currentJson.has("devDependencies") ? 
                    currentJson.getAsJsonObject("devDependencies") : new JsonObject();
            
            // Find new dependencies
            for (String dep : currentDeps) {
                if (!previousDeps.contains(dep)) {
                    String version = "";
                    if (dependencies.has(dep)) {
                        version = dependencies.get(dep).getAsString();
                    } else if (devDependencies.has(dep)) {
                        version = devDependencies.get(dep).getAsString();
                    }
                    newDependencies.add(new DependencyInfo(dep, "npm", version));
                }
            }
        } catch (Exception e) {
            // Log error but don't fail
            System.err.println("Error parsing npm dependencies: " + e.getMessage());
        }
        
        return newDependencies;
    }

    private Set<String> extractDependencies(String content) {
        Set<String> deps = new HashSet<>();
        if (content == null || content.trim().isEmpty()) {
            return deps;
        }
        
        try {
            JsonObject json = gson.fromJson(content, JsonObject.class);
            
            if (json.has("dependencies")) {
                JsonObject dependencies = json.getAsJsonObject("dependencies");
                dependencies.keySet().forEach(deps::add);
            }
            
            if (json.has("devDependencies")) {
                JsonObject devDependencies = json.getAsJsonObject("devDependencies");
                devDependencies.keySet().forEach(deps::add);
            }
        } catch (Exception e) {
            // Ignore parsing errors for previous content
        }
        
        return deps;
    }

    @Override
    public boolean supports(String fileName) {
        return "package.json".equals(fileName);
    }

    @Override
    public String getPackageManager() {
        return "npm";
    }
}
