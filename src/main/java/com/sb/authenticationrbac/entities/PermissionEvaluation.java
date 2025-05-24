package com.sb.authenticationrbac.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Document(collection = "permission_evaluations")
public class PermissionEvaluation {
    @Id
    private String id;
    private String userId;
    private String permissionId;
    private String resource;
    private String operation;
    private boolean allowed;
    private String reason;
    private Map<String, Object> context;
    private LocalDateTime evaluatedAt;
    
    // For audit and debugging
    // constructors, getters, setters
}