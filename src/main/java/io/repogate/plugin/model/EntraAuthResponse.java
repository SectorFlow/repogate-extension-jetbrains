package io.repogate.plugin.model;

/**
 * Response from /auth/entra/connect endpoint
 */
public class EntraAuthResponse {
    private String accessToken;
    private String tokenType;
    private int expiresIn;
    private String refreshToken;
    private UserInfo user;
    
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public String getTokenType() {
        return tokenType;
    }
    
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
    
    public int getExpiresIn() {
        return expiresIn;
    }
    
    public void setExpiresIn(int expiresIn) {
        this.expiresIn = expiresIn;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    public UserInfo getUser() {
        return user;
    }
    
    public void setUser(UserInfo user) {
        this.user = user;
    }
    
    public static class UserInfo {
        private String id;
        private String email;
        private String name;
        private String orgId;
        private String authMode;
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getOrgId() {
            return orgId;
        }
        
        public void setOrgId(String orgId) {
            this.orgId = orgId;
        }
        
        public String getAuthMode() {
            return authMode;
        }
        
        public void setAuthMode(String authMode) {
            this.authMode = authMode;
        }
    }
}
