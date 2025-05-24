package com.sb.authenticationrbac.rolesync.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleDefinitionsRootInput {
    private List<RoleDefinitionInput> roles;
}
