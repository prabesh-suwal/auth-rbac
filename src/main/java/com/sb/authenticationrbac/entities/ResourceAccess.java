package com.sb.authenticationrbac.entities;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ResourceAccess {
    private String resourceType;
    private String resourceId;
    private String accessType; // "GRANT", "DENY"
    private List<String> allowedOperations;
    private LocalDateTime expiresAt;
    
    // constructors, getters, setters
}