package com.sb.authenticationrbac.dtos.requests;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CreateRoleRequest {
    private String name;
    private String description;
    private List<String> permissionIds;
    private Map<String, Object> configuration;
    
    // constructors, getters, setters
}
