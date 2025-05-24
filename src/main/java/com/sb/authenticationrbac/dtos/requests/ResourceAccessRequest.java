package com.sb.authenticationrbac.dtos.requests;

import java.time.LocalDateTime;
import java.util.List;

public class ResourceAccessRequest {
    private String resourceType;
    private String resourceId;
    private List<String> operations;
    private LocalDateTime expiresAt;
    
    // getters and setters
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    public List<String> getOperations() { return operations; }
    public void setOperations(List<String> operations) { this.operations = operations; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
