package io.repogate.plugin.auth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * OAuth service for EntraID authentication
 * Uses local HTTP server to receive OAuth callback
 */
public class OAuthService {
    private static final int CALLBACK_PORT = 8765;
    private static final String CALLBACK_PATH = "/auth-callback";
    private static final int TIMEOUT_SECONDS = 300; // 5 minutes
    
    private final Gson gson = new Gson();
    private HttpServer callbackServer;
    private CompletableFuture<OAuthTokens> tokenFuture;
    
    /**
     * OAuth tokens result
     */
    public static class OAuthTokens {
        public final String accessToken;
        public final String refreshToken;
        
        public OAuthTokens(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }
    
    /**
     * Start OAuth authentication flow
     */
    @Nullable
    public OAuthTokens authenticate(String tenantId, String clientId, String redirectUri) {
        try {
            System.out.println("RepoGate: Starting EntraID OAuth authentication flow");
            System.out.println("RepoGate: Tenant ID: " + tenantId);
            System.out.println("RepoGate: Client ID: " + clientId);
            System.out.println("RepoGate: Redirect URI: " + redirectUri);
            
            // Generate PKCE challenge
            PKCEGenerator.PKCEChallenge pkce = PKCEGenerator.generate();
            
            // Build state parameter
            String state = buildState(clientId, pkce.codeVerifier, tenantId);
            
            // Build authorization URL
            String authUrl = buildAuthUrl(tenantId, clientId, redirectUri, pkce.codeChallenge, state);
            
            // Start local callback server
            startCallbackServer();
            
            // Open browser
            System.out.println("RepoGate: Opening browser for EntraID authentication");
            BrowserUtil.browse(authUrl);
            
            // Wait for callback with progress dialog
            return ProgressManager.getInstance().run(new Task.WithResult<OAuthTokens, Exception>(
                    null,
                    "RepoGate: Waiting for EntraID Sign-In",
                    true
            ) {
                @Override
                protected OAuthTokens compute(@NotNull ProgressIndicator indicator) throws Exception {
                    indicator.setText("Please complete sign-in in your browser...");
                    indicator.setIndeterminate(true);
                    
                    try {
                        return tokenFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        System.err.println("RepoGate: Authentication timed out or was cancelled");
                        return null;
                    }
                }
            });
            
        } catch (Exception e) {
            System.err.println("RepoGate: OAuth authentication error: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            stopCallbackServer();
        }
    }
    
    /**
     * Build state parameter
     */
    private String buildState(String clientId, String codeVerifier, String tenantId) {
        JsonObject stateObj = new JsonObject();
        stateObj.addProperty("clientType", "jetbrains");
        stateObj.addProperty("codeVerifier", codeVerifier);
        stateObj.addProperty("tenantId", tenantId);
        
        String stateJson = gson.toJson(stateObj);
        return java.util.Base64.getEncoder().encodeToString(stateJson.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Build OAuth authorization URL
     */
    private String buildAuthUrl(String tenantId, String clientId, String redirectUri, 
                                String codeChallenge, String state) {
        StringBuilder url = new StringBuilder();
        url.append("https://login.microsoftonline.com/").append(tenantId)
           .append("/oauth2/v2.0/authorize?");
        url.append("client_id=").append(urlEncode(clientId));
        url.append("&response_type=code");
        url.append("&redirect_uri=").append(urlEncode(redirectUri));
        url.append("&response_mode=query");
        url.append("&scope=").append(urlEncode("openid profile email offline_access"));
        url.append("&state=").append(urlEncode(state));
        url.append("&code_challenge=").append(urlEncode(codeChallenge));
        url.append("&code_challenge_method=S256");
        
        System.out.println("RepoGate: OAuth authorization URL built");
        return url.toString();
    }
    
    /**
     * Start local HTTP server to receive OAuth callback
     */
    private void startCallbackServer() throws IOException {
        tokenFuture = new CompletableFuture<>();
        
        callbackServer = HttpServer.create(new InetSocketAddress(CALLBACK_PORT), 0);
        callbackServer.createContext(CALLBACK_PATH, this::handleCallback);
        callbackServer.setExecutor(null);
        callbackServer.start();
        
        System.out.println("RepoGate: Callback server started on port " + CALLBACK_PORT);
    }
    
    /**
     * Stop local HTTP server
     */
    private void stopCallbackServer() {
        if (callbackServer != null) {
            callbackServer.stop(0);
            callbackServer = null;
            System.out.println("RepoGate: Callback server stopped");
        }
    }
    
    /**
     * Handle OAuth callback from backend
     */
    private void handleCallback(HttpExchange exchange) throws IOException {
        try {
            String query = exchange.getRequestURI().getQuery();
            System.out.println("RepoGate: OAuth callback received");
            System.out.println("RepoGate: Query: " + query);
            
            Map<String, String> params = parseQueryString(query);
            
            // Check for error
            if (params.containsKey("error")) {
                String error = params.get("error");
                String message = params.get("message");
                System.err.println("RepoGate: OAuth error: " + error + " - " + message);
                
                sendResponse(exchange, 400, "<html><body><h1>Authentication Failed</h1><p>" + 
                        (message != null ? message : error) + 
                        "</p><p>You can close this window.</p></body></html>");
                
                tokenFuture.complete(null);
                return;
            }
            
            // Extract tokens
            String token = params.get("token");
            String refreshToken = params.get("refreshToken");
            
            if (token != null) {
                System.out.println("RepoGate: Entra ID token extracted from callback");
                System.out.println("RepoGate: Token length: " + token.length());
                
                if (refreshToken != null) {
                    System.out.println("RepoGate: Refresh token extracted from callback");
                    System.out.println("RepoGate: Refresh token length: " + refreshToken.length());
                } else {
                    System.out.println("RepoGate: No refresh token in callback URL");
                }
                
                sendResponse(exchange, 200, "<html><body><h1>Authentication Successful!</h1>" +
                        "<p>You have been signed in to RepoGate.</p>" +
                        "<p>You can close this window and return to IntelliJ IDEA.</p></body></html>");
                
                tokenFuture.complete(new OAuthTokens(token, refreshToken));
            } else {
                System.err.println("RepoGate: No token in callback URL");
                
                sendResponse(exchange, 400, "<html><body><h1>Authentication Failed</h1>" +
                        "<p>No token received from server.</p>" +
                        "<p>You can close this window.</p></body></html>");
                
                tokenFuture.complete(null);
            }
            
        } catch (Exception e) {
            System.err.println("RepoGate: Error handling callback: " + e.getMessage());
            e.printStackTrace();
            
            sendResponse(exchange, 500, "<html><body><h1>Error</h1><p>" + 
                    e.getMessage() + "</p></body></html>");
            
            tokenFuture.complete(null);
        }
    }
    
    /**
     * Parse query string into map
     */
    private Map<String, String> parseQueryString(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return params;
        }
        
        // Decode the query string first (backend may double-encode)
        String decodedQuery = URLDecoder.decode(query, StandardCharsets.UTF_8);
        
        for (String param : decodedQuery.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2) {
                params.put(pair[0], pair[1]);
            }
        }
        
        return params;
    }
    
    /**
     * Send HTTP response
     */
    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
    
    /**
     * URL encode
     */
    private String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
