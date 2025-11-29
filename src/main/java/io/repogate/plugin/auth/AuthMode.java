package io.repogate.plugin.auth;

/**
 * Authentication modes supported by RepoGate
 */
public enum AuthMode {
    /**
     * EntraID SSO authentication (OAuth2)
     */
    ENTRA_SSO,
    
    /**
     * Local API token authentication (legacy)
     */
    LOCAL_TOKEN,
    
    /**
     * Not authenticated
     */
    UNAUTHENTICATED
}
