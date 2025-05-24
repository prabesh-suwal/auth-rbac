package com.sb.authenticationrbac.rolesync.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleDefinitionInput {
    private String name;
    private String description;
    private String parentRoleName; // Optional: for role hierarchy
    private List<String> permissions;
    private RoleConfigurationInput configuration;
    private boolean active = true; // Default to true as per common practice
}
