package com.sb.authenticationrbac.services;

import com.sb.authenticationrbac.entities.Role;
import com.sb.authenticationrbac.repositories.PermissionRepository;
import com.sb.authenticationrbac.repositories.RoleRepository;
import com.sb.authenticationrbac.role.dto.RoleDefinition;
import com.sb.authenticationrbac.validator.ValidatorDispatcher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RoleService {
    private final ValidatorDispatcher validator;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleService(ValidatorDispatcher validator, RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.validator = validator;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    public Role createRole(RoleDefinition roleDefinition) {
        // Validate the role definition
        this.validator.validate(roleDefinition);
        Role role = new Role();
        role.setId(UUID.randomUUID().toString());
        role.setName(roleDefinition.getName());
        role.setDescription(roleDefinition.getDescription());
        role.setPermissions(this.permissionRepository.findByIdIn(roleDefinition.getPermissionIds()));
        role.setConfiguration(roleDefinition.getConfiguration());
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        role.setActive(roleDefinition.isActive());

        return this.roleRepository.save(role);

        // Proceed with role creation if validation passes
        // ...
    }

}
