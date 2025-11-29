package io.repogate.plugin.model;

/**
 * Response from /auth/mode endpoint
 */
public class AuthModeResponse {
    private String authMode; // "ENTRA_SSO" or "LOCAL_TOKEN"
    private String organizationName;
    private boolean requiresEntraSso;
    private String tenantId;
    private String clientId;
    private String redirectUri;
    
    public String getAuthMode() {
        return authMode;
    }
    
    public void setAuthMode(String authMode) {
        this.authMode = authMode;
    }
    
    public String getOrganizationName() {
        return organizationName;
    }
    
    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }
    
    public boolean isRequiresEntraSso() {
        return requiresEntraSso;
    }
    
    public void setRequiresEntraSso(boolean requiresEntraSso) {
        this.requiresEntraSso = requiresEntraSso;
    }
    
    public String getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    public String getRedirectUri() {
        return redirectUri;
    }
    
    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }
}
