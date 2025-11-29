package io.repogate.plugin.model;

/**
 * User information
 */
public class UserInfo {
    private String id;
    private String email;
    private String name;
    private String orgId;
    private String authMode;
    
    public UserInfo() {
    }
    
    public UserInfo(String id, String email, String name, String orgId, String authMode) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.orgId = orgId;
        this.authMode = authMode;
    }
    
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
