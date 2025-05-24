package com.sb.authenticationrbac.services;

import com.sb.authenticationrbac.entities.Role;
import com.sb.authenticationrbac.repositories.RoleRepository;
import com.sb.authenticationrbac.role.dto.RoleDefinition;
import com.sb.authenticationrbac.validator.role.CreateRoleValidator;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RoleService {
    private final CreateRoleValidator validator;
    private final RoleRepository roleRepository;

    public RoleService(CreateRoleValidator validator, RoleRepository roleRepository) {
        this.validator = validator;
        this.roleRepository = roleRepository;
    }

    public Role createRole(RoleDefinition roleDefinition) {
        // Validate the role definition
        validator.validate(roleDefinition);
        Role role = new Role();
        role.setId(UUID.randomUUID().toString());
        role.setName(roleDefinition.getName());
        role.setDescription(roleDefinition.getDescription());
        role.setPermissions(roleDefinition.getPermissionIds());
        role.setConfiguration(roleDefinition.getConfiguration());
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        role.setActive(roleDefinition.isActive());

        return this.roleRepository.save(role);

        // Proceed with role creation if validation passes
        // ...
    }

}
