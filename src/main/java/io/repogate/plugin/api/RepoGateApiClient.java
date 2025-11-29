package io.repogate.plugin.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class RepoGateApiClient {
    private static final String DEFAULT_BASE_URL = "https://app.repogate.io/api/v1";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private final OkHttpClient client;
    private final Gson gson;
    private final String baseUrl;
    private final String apiToken;

    public RepoGateApiClient(String baseUrl, String apiToken) {
        this.baseUrl = baseUrl != null && !baseUrl.isEmpty() ? baseUrl : DEFAULT_BASE_URL;
        this.apiToken = apiToken;
        this.gson = new Gson();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Request validation for a new dependency
     * Aligned with VS Code: /request endpoint
     */
    public DependencyResponse requestDependency(String name, String ecosystem, String version, String projectName, String path, boolean repository) throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("name", name);
        requestBody.addProperty("ecosystem", ecosystem); // npm, maven, gradle
        requestBody.addProperty("version", version);
        requestBody.addProperty("projectName", projectName);
        requestBody.addProperty("path", path);
        requestBody.addProperty("repository", repository);

        RequestBody body = RequestBody.create(gson.toJson(requestBody), JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/request") // Changed from /dependencies/request
                .post(body)
                .addHeader("Authorization", "Bearer " + apiToken)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response.code());
            }
            
            String responseBody = response.body() != null ? response.body().string() : "{}";
            return gson.fromJson(responseBody, DependencyResponse.class);
        }
    }

    /**
     * Check the approval status of a dependency
     * Aligned with VS Code: /check endpoint
     */
    public DependencyResponse checkDependency(String name, String ecosystem, String version, String projectName, boolean repository) throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("name", name);
        requestBody.addProperty("ecosystem", ecosystem);
        requestBody.addProperty("version", version);
        requestBody.addProperty("projectName", projectName);
        requestBody.addProperty("repository", repository);

        RequestBody body = RequestBody.create(gson.toJson(requestBody), JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/check") // Changed from /dependencies/check
                .post(body)
                .addHeader("Authorization", "Bearer " + apiToken)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response.code());
            }
            
            String responseBody = response.body() != null ? response.body().string() : "{}";
            return gson.fromJson(responseBody, DependencyResponse.class);
        }
    }
    
    /**
     * Update dependency status (removal or version change)
     * Aligned with VS Code: /update endpoint
     */
    public void updateDependency(String name, String ecosystem, String fromVersion, String toVersion, String action, String projectName, boolean repository) throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("name", name);
        requestBody.addProperty("ecosystem", ecosystem);
        requestBody.addProperty("fromVersion", fromVersion);
        if (toVersion != null) {
            requestBody.addProperty("toVersion", toVersion);
        }
        requestBody.addProperty("action", action); // "removed" or "updated"
        requestBody.addProperty("projectName", projectName);
        requestBody.addProperty("timestamp", java.time.Instant.now().toString());
        requestBody.addProperty("repository", repository);

        RequestBody body = RequestBody.create(gson.toJson(requestBody), JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/update")
                .post(body)
                .addHeader("Authorization", "Bearer " + apiToken)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println("RepoGate: Failed to update dependency: " + response.code());
            }
        }
    }

    /**
     * Report inventory of all dependencies to RepoGate
     */
    public void reportInventory(java.util.List<io.repogate.plugin.model.DependencyInfo> dependencies, java.util.Map<String, String> developerInfo) {
        try {
            JsonObject payload = new JsonObject();
            com.google.gson.JsonArray depsArray = new com.google.gson.JsonArray();
            
            for (io.repogate.plugin.model.DependencyInfo dep : dependencies) {
                JsonObject depObj = new JsonObject();
                depObj.addProperty("packageName", dep.getPackageName());
                depObj.addProperty("packageManager", dep.getPackageManager());
                depObj.addProperty("status", dep.getStatus().toString());
                depsArray.add(depObj);
            }
            
            JsonObject devInfo = new JsonObject();
            for (java.util.Map.Entry<String, String> entry : developerInfo.entrySet()) {
                devInfo.addProperty(entry.getKey(), entry.getValue());
            }
            
            payload.add("dependencies", depsArray);
            payload.add("developer", devInfo);
            payload.addProperty("timestamp", java.time.Instant.now().toString());
            
            RequestBody body = RequestBody.create(gson.toJson(payload), JSON);
            Request request = new Request.Builder()
                    .url(baseUrl + "/dependencies/inventory")
                    .post(body)
                    .addHeader("Authorization", "Bearer " + apiToken)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("RepoGate: Failed to report inventory: " + response.code());
                }
            }
        } catch (Exception e) {
            System.err.println("RepoGate: Error reporting inventory: " + e.getMessage());
        }
    }

    /**
     * Queue packages for initial scan
     */
    public void queuePackages(java.util.List<PackageInfo> packages) {
        try {
            JsonObject payload = new JsonObject();
            com.google.gson.JsonArray packagesArray = new com.google.gson.JsonArray();
            
            for (PackageInfo pkg : packages) {
                JsonObject pkgObj = new JsonObject();
                pkgObj.addProperty("packageName", pkg.packageName);
                if (pkg.packageVersion != null && !pkg.packageVersion.isEmpty()) {
                    pkgObj.addProperty("packageVersion", pkg.packageVersion);
                }
                pkgObj.addProperty("packageManager", pkg.packageManager);
                if (pkg.projectName != null && !pkg.projectName.isEmpty()) {
                    pkgObj.addProperty("projectName", pkg.projectName);
                }
                packagesArray.add(pkgObj);
            }
            
            payload.add("packages", packagesArray);
            
            RequestBody body = RequestBody.create(gson.toJson(payload), JSON);
            Request request = new Request.Builder()
                    .url(baseUrl + "/queue")
                    .post(body)
                    .addHeader("Authorization", "Bearer " + apiToken)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("RepoGate: Failed to queue packages: " + response.code());
                }
            }
        } catch (Exception e) {
            System.err.println("RepoGate: Error queuing packages: " + e.getMessage());
        }
    }

    public static class PackageInfo {
        public String packageName;
        public String packageVersion;
        public String packageManager;
        public String projectName;
        
        public PackageInfo(String packageName, String packageVersion, String packageManager, String projectName) {
            this.packageName = packageName;
            this.packageVersion = packageVersion;
            this.packageManager = packageManager;
            this.projectName = projectName;
        }
    }

    public static class DependencyResponse {
        private boolean approved;
        private String message;
        private String packageName;
        private String packageManager;
        private String status; // "approved", "denied", "pending", "scanning", "not_found"

        public boolean isApproved() {
            return approved;
        }

        public void setApproved(boolean approved) {
            this.approved = approved;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getPackageName() {
            return packageName;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        public String getPackageManager() {
            return packageManager;
        }

        public void setPackageManager(String packageManager) {
            this.packageManager = packageManager;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
