package com.sb.authenticationrbac.permissionsync.dto;

import com.sb.authenticationrbac.entities.PermissionConfig;
import lombok.Data;

@Data
public class PermissionDefinition {
    private String name;
    private String resource;
    private String operation;
    private String description;
    private ApiDefinition api;
    private PermissionConfig config;
    private AccessControlDefinition accessControl;
    private boolean active;
} 