package com.sb.authenticationrbac.permissionsync.dto;

import lombok.Data;
import java.util.Map;

@Data
public class PermissionDefinitionsRoot {
    private Map<String, ApiGroupDefinition> apiGroups;
} 