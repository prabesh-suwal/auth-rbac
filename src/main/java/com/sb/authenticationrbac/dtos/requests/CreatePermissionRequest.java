package com.sb.authenticationrbac.dtos.requests;

import com.sb.authenticationrbac.entities.PermissionConfig;
import lombok.Data;

@Data
public class CreatePermissionRequest {
    private String name;
    private String resource;
    private String action;
    private String description;
    private PermissionConfig config;
    
    // constructors, getters, setters
}