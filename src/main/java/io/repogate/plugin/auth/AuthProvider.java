package io.repogate.plugin.auth;

import org.jetbrains.annotations.Nullable;

/**
 * Interface for authentication providers
 */
public interface AuthProvider {
    /**
     * Get the authentication token
     * @return The authentication token, or null if not available
     */
    @Nullable
    String getToken();

    /**
     * Check if authentication is configured
     * @return true if authentication is configured and ready
     */
    boolean isConfigured();

    /**
     * Get the authentication type
     * @return The authentication type (e.g., "API Token", "EntraID")
     */
    String getAuthType();

    /**
     * Authenticate the user
     * @return true if authentication was successful
     */
    boolean authenticate();

    /**
     * Clear authentication credentials
     */
    void clearAuth();

    /**
     * Refresh the authentication token if needed
     * @return true if token was refreshed successfully
     */
    boolean refreshTokenIfNeeded();
}
