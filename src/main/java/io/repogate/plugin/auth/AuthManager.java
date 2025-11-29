package io.repogate.plugin.auth;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import io.repogate.plugin.settings.RepoGateSettings;
import org.jetbrains.annotations.Nullable;

/**
 * Main authentication manager
 * Coordinates between different authentication providers
 */
@Service
public final class AuthManager {
    private AuthProvider currentProvider;
    
    public static AuthManager getInstance() {
        return ApplicationManager.getApplication().getService(AuthManager.class);
    }
    
    /**
     * Get current authentication provider based on settings
     */
    @Nullable
    public AuthProvider getCurrentProvider() {
        if (currentProvider != null) {
            return currentProvider;
        }
        
        RepoGateSettings settings = RepoGateSettings.getInstance();
        AuthMode authMode = settings.getAuthMode();
        
        switch (authMode) {
            case ENTRA_SSO:
                currentProvider = new EntraIdAuthProvider(settings.getApiUrl());
                break;
            case LOCAL_TOKEN:
                currentProvider = new ApiTokenAuthProvider();
                break;
            default:
                return null;
        }
        
        return currentProvider;
    }
    
    /**
     * Get current authentication token
     */
    @Nullable
    public String getToken() {
        AuthProvider provider = getCurrentProvider();
        if (provider != null && provider.isConfigured()) {
            // Refresh token if needed (for EntraID)
            provider.refreshTokenIfNeeded();
            return provider.getToken();
        }
        return null;
    }
    
    /**
     * Check if user is authenticated
     */
    public boolean isAuthenticated() {
        AuthProvider provider = getCurrentProvider();
        return provider != null && provider.isConfigured();
    }
    
    /**
     * Sign in with EntraID
     */
    public boolean signInWithEntraID() {
        RepoGateSettings settings = RepoGateSettings.getInstance();
        EntraIdAuthProvider provider = new EntraIdAuthProvider(settings.getApiUrl());
        
        if (provider.authenticate()) {
            settings.setAuthMode(AuthMode.ENTRA_SSO);
            currentProvider = provider;
            return true;
        }
        
        return false;
    }
    
    /**
     * Sign in with API Token
     */
    public boolean signInWithAPIToken() {
        ApiTokenAuthProvider provider = new ApiTokenAuthProvider();
        
        if (provider.authenticate()) {
            RepoGateSettings settings = RepoGateSettings.getInstance();
            settings.setAuthMode(AuthMode.LOCAL_TOKEN);
            currentProvider = provider;
            return true;
        }
        
        return false;
    }
    
    /**
     * Sign out
     */
    public void signOut() {
        AuthProvider provider = getCurrentProvider();
        if (provider != null) {
            provider.clearAuth();
        }
        
        RepoGateSettings settings = RepoGateSettings.getInstance();
        settings.setAuthMode(AuthMode.UNAUTHENTICATED);
        currentProvider = null;
    }
    
    /**
     * Get authentication mode
     */
    public AuthMode getAuthMode() {
        return RepoGateSettings.getInstance().getAuthMode();
    }
    
    /**
     * Get authentication status string
     */
    public String getAuthStatus() {
        if (!isAuthenticated()) {
            return "Not signed in";
        }
        
        AuthProvider provider = getCurrentProvider();
        if (provider != null) {
            return "Signed in (" + provider.getAuthType() + ")";
        }
        
        return "Unknown";
    }
    
    /**
     * Get user information
     */
    @Nullable
    public io.repogate.plugin.model.UserInfo getUserInfo() {
        AuthProvider provider = getCurrentProvider();
        if (provider instanceof EntraIdAuthProvider) {
            return ((EntraIdAuthProvider) provider).getUserInfo();
        }
        return null;
    }
    
    /**
     * Get authentication type
     */
    @Nullable
    public String getAuthType() {
        AuthProvider provider = getCurrentProvider();
        if (provider != null) {
            return provider.getAuthType();
        }
        return null;
    }
}
