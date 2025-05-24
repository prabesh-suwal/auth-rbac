package com.sb.authenticationrbac.validator.role;

import com.sb.authenticationrbac.exception.ValidationException;
import com.sb.authenticationrbac.role.dto.RoleDefinition;
import com.sb.authenticationrbac.validator.Validator;
import org.springframework.stereotype.Component;

@Component
public class RoleNameValidator implements Validator<RoleDefinition> {
    private static final String NAME_PATTERN = "^[A-Z][A-Z0-9_]*$";

    @Override
    public void validate(RoleDefinition role) throws ValidationException {
        String name = role.getName();
        
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Role name is required");
        }

        if (!name.matches(NAME_PATTERN)) {
            throw new ValidationException("Role name must be uppercase with underscores and numbers only");
        }

        if (role.getDescription() == null || role.getDescription().trim().isEmpty()) {
            throw new ValidationException("Role description is required");
        }

        if (role.getDescription().length() < 10 || role.getDescription().length() > 200) {
            throw new ValidationException("Description must be between 10 and 200 characters");
        }
    }
} 