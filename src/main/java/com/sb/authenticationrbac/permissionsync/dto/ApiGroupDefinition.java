package com.sb.authenticationrbac.permissionsync.dto;

import lombok.Data;
import java.util.List;

@Data
public class ApiGroupDefinition {
    private String description;
    private List<PermissionDefinition> permissions;
} 