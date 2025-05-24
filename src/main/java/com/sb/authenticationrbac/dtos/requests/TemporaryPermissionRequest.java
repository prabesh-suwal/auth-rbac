package com.sb.authenticationrbac.dtos.requests;


import java.time.LocalDateTime;
import java.util.Map;

public class TemporaryPermissionRequest {
    private String permissionId;
    private LocalDateTime expiresAt;
    private String reason;
    private Map<String, Object> conditions;
    
    // getters and setters
    public String getPermissionId() { return permissionId; }
    public void setPermissionId(String permissionId) { this.permissionId = permissionId; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Map<String, Object> getConditions() { return conditions; }
    public void setConditions(Map<String, Object> conditions) { this.conditions = conditions; }
}