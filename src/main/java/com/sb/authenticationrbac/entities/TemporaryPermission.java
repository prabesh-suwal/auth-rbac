package com.sb.authenticationrbac.entities;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class TemporaryPermission {
    private String permissionId;
    private LocalDateTime grantedAt;
    private LocalDateTime expiresAt;
    private String grantedBy;
    private String reason;
    private Map<String, Object> conditions;
    
    // constructors, getters, setters
}
