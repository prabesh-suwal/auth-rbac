package com.sb.authenticationrbac.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "permissions")
public class Permission {
    @Id
    private String id;
    private String name; // e.g., "LOAN_APPROVE", "LOAN_VIEW"
    private String resource; // e.g., "LOAN", "USER", "BRANCH"
    private String action; // e.g., "CREATE", "READ", "UPDATE", "DELETE", "APPROVE"
    private String description;
    
    // Dynamic permission configuration
    private PermissionConfig config;
    
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // constructors, getters, setters
}