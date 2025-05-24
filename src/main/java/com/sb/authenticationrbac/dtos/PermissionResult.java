package com.sb.authenticationrbac.dtos;

import lombok.Data;

import java.util.Map;

@Data

// Result class for permission evaluation
public class PermissionResult {
    private boolean allowed;
    private String reason;
    private Map<String, Object> additionalInfo;
    
    public static PermissionResult allowed(String reason) {
        return new PermissionResult(true, reason, null);
    }
    
    public static PermissionResult denied(String reason) {
        return new PermissionResult(false, reason, null);
    }
    
    public PermissionResult(boolean allowed, String reason, Map<String, Object> additionalInfo) {
        this.allowed = allowed;
        this.reason = reason;
        this.additionalInfo = additionalInfo;
    }
    
    // getters and setters
}