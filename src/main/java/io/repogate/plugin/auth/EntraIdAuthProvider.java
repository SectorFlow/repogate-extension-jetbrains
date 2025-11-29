package io.repogate.plugin.auth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.Messages;
import io.repogate.plugin.model.EntraAuthResponse;
import io.repogate.plugin.model.TokenRefreshResponse;
import okhttp3.*;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * EntraID OAuth authentication provider
 */
public class EntraIdAuthProvider implements AuthProvider {
    private final String apiUrl;
    private final OkHttpClient client;
    private final Gson gson;
    private final OAuthService oauthService;
    
    // Token expiration tracking
    private long tokenExpiration = 0;
    
    public EntraIdAuthProvider(String apiUrl) {
        this.apiUrl = apiUrl;
        this.gson = new Gson();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.oauthService = new OAuthService();
        
        // Load expiration from storage if available
        String expirationStr = SecureStorage.get("repogate.tokenExpiration");
        if (expirationStr != null) {
            try {
                tokenExpiration = Long.parseLong(expirationStr);
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
    }
    
    @Override
    @Nullable
    public String getToken() {
        return SecureStorage.get(SecureStorage.ACCESS_TOKEN_KEY);
    }
    
    @Override
    public boolean isConfigured() {
        String token = getToken();
        return token != null && !token.trim().isEmpty();
    }
    
    @Override
    public String getAuthType() {
        return "EntraID SSO";
    }
    
    @Override
    public boolean authenticate() {
        try {
            // Prompt for email
            String email = Messages.showInputDialog(
                    "Enter your email address to sign in to RepoGate:",
                    "RepoGate EntraID Sign-In",
                    null,
                    "",
                    null
            );
            
            if (email == null || email.trim().isEmpty()) {
                return false;
            }
            
            // Discover auth mode
            String authModeUrl = apiUrl + "/auth/mode?email=" + urlEncode(email);
            Request request = new Request.Builder()
                    .url(authModeUrl)
                    .get()
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    Messages.showErrorDialog(
                            "Failed to determine authentication method: " + response.code(),
                            "Authentication Error"
                    );
                    return false;
                }
                
                String responseBody = response.body() != null ? response.body().string() : "{}";
                JsonObject authModeData = gson.fromJson(responseBody, JsonObject.class);
                
                String authMode = authModeData.get("authMode").getAsString();
                if (!"ENTRA_SSO".equals(authMode)) {
                    Messages.showWarningDialog(
                            "Your organization uses API Token authentication. Please use \"Sign In with API Token\" instead.",
                            "Wrong Authentication Method"
                    );
                    return false;
                }
                
                String tenantId = authModeData.get("tenantId").getAsString();
                String clientId = authModeData.get("clientId").getAsString();
                String redirectUri = authModeData.get("redirectUri").getAsString();
                
                System.out.println("RepoGate: Starting EntraID authentication flow");
                
                // Start OAuth flow
                OAuthService.OAuthTokens tokens = oauthService.authenticate(tenantId, clientId, redirectUri);
                
                if (tokens == null || tokens.accessToken == null) {
                    System.out.println("RepoGate: EntraID authentication cancelled or failed");
                    return false;
                }
                
                // Store refresh token if available
                if (tokens.refreshToken != null) {
                    SecureStorage.store(SecureStorage.REFRESH_TOKEN_KEY, tokens.refreshToken);
                    System.out.println("RepoGate: Refresh token stored from OAuth callback");
                } else {
                    System.out.println("RepoGate: No refresh token received from OAuth callback");
                }
                
                // Exchange Entra token for RepoGate JWT
                EntraAuthResponse authResponse = exchangeEntraToken(tokens.accessToken);
                if (authResponse == null) {
                    return false;
                }
                
                // Store authentication data
                storeEntraAuth(authResponse);
                
                System.out.println("RepoGate: EntraID authentication successful");
                Messages.showInfoMessage(
                        "Signed in as " + authResponse.getUser().getName() + " (" + authResponse.getUser().getEmail() + ")",
                        "Sign-In Successful"
                );
                
                return true;
            }
            
        } catch (Exception e) {
            System.err.println("RepoGate: EntraID authentication failed: " + e.getMessage());
            e.printStackTrace();
            Messages.showErrorDialog(
                    "Authentication failed: " + e.getMessage(),
                    "Authentication Error"
            );
            return false;
        }
    }
    
    @Override
    public void clearAuth() {
        SecureStorage.delete(SecureStorage.ACCESS_TOKEN_KEY);
        SecureStorage.delete(SecureStorage.REFRESH_TOKEN_KEY);
        SecureStorage.delete("repogate.tokenExpiration");
        tokenExpiration = 0;
    }
    
    @Override
    public boolean refreshTokenIfNeeded() {
        // Check if token is expiring soon (within 5 minutes)
        long now = System.currentTimeMillis();
        if (tokenExpiration > 0 && (tokenExpiration - now) < 300000) {
            System.out.println("RepoGate: Token expiring soon, attempting refresh...");
            return refreshToken();
        }
        return true;
    }
    
    /**
     * Exchange Entra ID token for RepoGate JWT
     */
    @Nullable
    private EntraAuthResponse exchangeEntraToken(String entraToken) {
        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("client", "intellij");
            requestBody.addProperty("extensionVersion", "1.4.0"); // TODO: Get from plugin.xml
            
            RequestBody body = RequestBody.create(
                    gson.toJson(requestBody),
                    MediaType.get("application/json; charset=utf-8")
            );
            
            Request request = new Request.Builder()
                    .url(apiUrl + "/auth/entra/connect")
                    .post(body)
                    .addHeader("Authorization", "Bearer " + entraToken)
                    .addHeader("Content-Type", "application/json")
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorMsg = "Token exchange failed: " + response.code();
                    if (response.body() != null) {
                        errorMsg += " - " + response.body().string();
                    }
                    Messages.showErrorDialog(errorMsg, "Authentication Error");
                    return null;
                }
                
                String responseBody = response.body() != null ? response.body().string() : "{}";
                return gson.fromJson(responseBody, EntraAuthResponse.class);
            }
            
        } catch (Exception e) {
            System.err.println("RepoGate: Token exchange failed: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Store EntraID authentication data
     */
    private void storeEntraAuth(EntraAuthResponse authResponse) {
        // Store access token
        SecureStorage.store(SecureStorage.ACCESS_TOKEN_KEY, authResponse.getAccessToken());
        
        // Store refresh token if available
        if (authResponse.getRefreshToken() != null) {
            SecureStorage.store(SecureStorage.REFRESH_TOKEN_KEY, authResponse.getRefreshToken());
            System.out.println("RepoGate: Refresh token stored successfully");
        }
        
        // Store user info if available
        if (authResponse.getUser() != null) {
            String userInfoJson = gson.toJson(authResponse.getUser());
            SecureStorage.store("repogate.userInfo", userInfoJson);
            System.out.println("RepoGate: User info stored successfully");
        }
        
        // Calculate and store expiration timestamp
        long expirationTime = System.currentTimeMillis() + (authResponse.getExpiresIn() * 1000L);
        tokenExpiration = expirationTime;
        SecureStorage.store("repogate.tokenExpiration", String.valueOf(expirationTime));
        
        System.out.println("RepoGate: EntraID auth data stored successfully");
    }
    
    /**
     * Refresh expired JWT token using refresh token
     */
    private boolean refreshToken() {
        try {
            String refreshToken = SecureStorage.get(SecureStorage.REFRESH_TOKEN_KEY);
            if (refreshToken == null) {
                System.out.println("RepoGate: No refresh token available");
                return false;
            }
            
            String accessToken = SecureStorage.get(SecureStorage.ACCESS_TOKEN_KEY);
            
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("refreshToken", refreshToken);
            
            RequestBody body = RequestBody.create(
                    gson.toJson(requestBody),
                    MediaType.get("application/json; charset=utf-8")
            );
            
            Request request = new Request.Builder()
                    .url(apiUrl + "/auth/entra/refresh")
                    .post(body)
                    .addHeader("Authorization", "Bearer " + (accessToken != null ? accessToken : ""))
                    .addHeader("Content-Type", "application/json")
                    .build();
            
            System.out.println("RepoGate: Refreshing access token using refresh token");
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("RepoGate: Token refresh failed: " + response.code());
                    return false;
                }
                
                String responseBody = response.body() != null ? response.body().string() : "{}";
                TokenRefreshResponse refreshResponse = gson.fromJson(responseBody, TokenRefreshResponse.class);
                
                // Store new access token
                SecureStorage.store(SecureStorage.ACCESS_TOKEN_KEY, refreshResponse.getAccessToken());
                
                // Store new refresh token if provided (token rotation)
                if (refreshResponse.getRefreshToken() != null) {
                    SecureStorage.store(SecureStorage.REFRESH_TOKEN_KEY, refreshResponse.getRefreshToken());
                    System.out.println("RepoGate: New refresh token stored (token rotation)");
                }
                
                // Update expiration time
                long expirationTime = System.currentTimeMillis() + (refreshResponse.getExpiresIn() * 1000L);
                tokenExpiration = expirationTime;
                SecureStorage.store("repogate.tokenExpiration", String.valueOf(expirationTime));
                
                System.out.println("RepoGate: Token refreshed successfully");
                return true;
            }
            
        } catch (Exception e) {
            System.err.println("RepoGate: Token refresh failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * URL encode
     */
    private String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
    
    /**
     * Get user information from stored auth data
     */
    @Nullable
    public io.repogate.plugin.model.UserInfo getUserInfo() {
        String userInfoJson = SecureStorage.get("repogate.userInfo");
        if (userInfoJson != null) {
            try {
                return gson.fromJson(userInfoJson, io.repogate.plugin.model.UserInfo.class);
            } catch (Exception e) {
                System.err.println("RepoGate: Failed to parse user info: " + e.getMessage());
            }
        }
        return null;
    }
}
