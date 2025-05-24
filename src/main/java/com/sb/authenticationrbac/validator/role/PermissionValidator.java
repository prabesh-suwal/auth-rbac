package com.sb.authenticationrbac.validator.role;

import com.sb.authenticationrbac.exception.ValidationException;
import com.sb.authenticationrbac.role.dto.RoleDefinition;
import com.sb.authenticationrbac.validator.Validator;
import com.sb.authenticationrbac.repositories.PermissionRepository;
import org.springframework.stereotype.Component;

@Component
public class PermissionValidator implements Validator<RoleDefinition> {
    private final PermissionRepository permissionRepository;

    public PermissionValidator(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Override
    public void validate(RoleDefinition role) throws ValidationException {
        if (role.getPermissionIds() == null || role.getPermissionIds().isEmpty()) {
            throw new ValidationException("At least one permission is required");
        }

        for (String permissionId : role.getPermissionIds()) {
            if (permissionId == null || permissionId.trim().isEmpty()) {
                throw new ValidationException("Permission ID cannot be blank");
            }
            
            if (!permissionRepository.existsById(permissionId)) {
                throw new ValidationException("Permission ID '" + permissionId + "' does not exist");
            }
        }
    }
} 