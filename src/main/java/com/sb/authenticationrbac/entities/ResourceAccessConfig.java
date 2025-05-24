package com.sb.authenticationrbac.entities;

import lombok.Data;

import java.util.List;

@Data
public class ResourceAccessConfig {
    private String accessType; // "ALL", "OWN", "SPECIFIC", "CONDITIONAL"
    private List<String> allowedResourceIds;
    private String ownershipFieldPath = "createdBy"; // Path to owner field
    private List<String> accessConditions; // SpEL expressions
    
    // constructors, getters, setters
}
