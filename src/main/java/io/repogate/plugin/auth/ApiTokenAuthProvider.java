package io.repogate.plugin.auth;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.Nullable;

/**
 * API Token authentication provider using SecureStorage
 */
public class ApiTokenAuthProvider implements AuthProvider {

    @Override
    @Nullable
    public String getToken() {
        return SecureStorage.get(SecureStorage.API_TOKEN_KEY);
    }

    @Override
    public boolean isConfigured() {
        String token = getToken();
        return token != null && !token.trim().isEmpty();
    }

    @Override
    public String getAuthType() {
        return "API Token";
    }

    @Override
    public boolean authenticate() {
        String token = Messages.showPasswordDialog(
                (Project) null,
                "Enter your RepoGate API token:",
                "RepoGate API Token",
                null,
                null
        );

        if (token != null && !token.trim().isEmpty()) {
            setToken(token);
            return true;
        }
        return false;
    }

    @Override
    public void clearAuth() {
        SecureStorage.delete(SecureStorage.API_TOKEN_KEY);
    }

    @Override
    public boolean refreshTokenIfNeeded() {
        // API tokens don't need refresh
        return true;
    }

    /**
     * Set the API token
     */
    public void setToken(String token) {
        SecureStorage.store(SecureStorage.API_TOKEN_KEY, token);
    }
}
