package io.repogate.plugin.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class RepoGateApiClient {
    private static final String DEFAULT_BASE_URL = "http://localhost:3000/api/v1";
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
     */
    public DependencyResponse requestDependency(String packageName, String packageManager) throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("packageName", packageName);
        requestBody.addProperty("packageManager", packageManager);

        RequestBody body = RequestBody.create(gson.toJson(requestBody), JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/dependencies/request")
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
     */
    public DependencyResponse checkDependency(String packageName, String packageManager) throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("packageName", packageName);
        requestBody.addProperty("packageManager", packageManager);

        RequestBody body = RequestBody.create(gson.toJson(requestBody), JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/dependencies/check")
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

    public static class DependencyResponse {
        private boolean approved;
        private String message;
        private String packageName;
        private String packageManager;
        private String status; // "approved", "denied", "pending"

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
