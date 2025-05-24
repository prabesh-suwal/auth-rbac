package com.sb.authenticationrbac.exceptions;

import java.util.Map;

// Enhanced custom exception with more details
public class PermissionDeniedException extends RuntimeException {
    private String permissionName;
    private String resource;
    private String operation;
    private Map<String, Object> context;
    
    public PermissionDeniedException(String message) {
        super(message);
    }
    
    public PermissionDeniedException(String message, String permissionName, String resource, String operation) {
        super(message);
        this.permissionName = permissionName;
        this.resource = resource;
        this.operation = operation;
    }
    
    public PermissionDeniedException(String message, String permissionName, String resource, 
                                   String operation, Map<String, Object> context) {
        super(message);
        this.permissionName = permissionName;
        this.resource = resource;
        this.operation = operation;
        this.context = context;
    }
    
    // getters and setters
    public String getPermissionName() { return permissionName; }
    public String getResource() { return resource; }
    public String getOperation() { return operation; }
    public Map<String, Object> getContext() { return context; }
}