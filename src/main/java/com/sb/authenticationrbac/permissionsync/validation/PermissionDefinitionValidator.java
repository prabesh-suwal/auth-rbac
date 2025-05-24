package com.sb.authenticationrbac.permissionsync.validation;

import com.sb.authenticationrbac.permissionsync.dto.PermissionDefinition;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class PermissionDefinitionValidator {
    private static final Set<String> VALID_HTTP_METHODS = new HashSet<>(
        Arrays.asList("*", "GET", "POST", "PUT", "DELETE", "PATCH")
    );
    
    private static final Set<String> VALID_BRANCH_ACCESS_TYPES = new HashSet<>(
        Arrays.asList("OWN_BRANCH", "ALL_BRANCHES", "SPECIFIC_BRANCHES", "BRANCH_HIERARCHY")
    );
    
    private static final Pattern TIME_WINDOW_PATTERN = Pattern.compile(
        "^([0-1][0-9]|2[0-3]):[0-5][0-9]$"
    );
    
    private static final Pattern PERMISSION_NAME_PATTERN = Pattern.compile(
        "^[A-Z][A-Z0-9_]{2,49}$"
    );

    public ValidationResult validate(PermissionDefinition definition) {
        ValidationResult result = ValidationResult.success();
        
        // Validate basic fields
        validateBasicFields(definition, result);
        
        // Validate API
        validateApi(definition, result);
        
        // Validate config
        validateConfig(definition, result);
        
        return result;
    }

    private void validateBasicFields(PermissionDefinition definition, ValidationResult result) {
        if (definition.getName() == null || definition.getName().isEmpty()) {
            result.addError("name", "Permission name is required");
        } else if (!PERMISSION_NAME_PATTERN.matcher(definition.getName()).matches()) {
            result.addError("name", "Permission name must be uppercase, start with a letter, and contain only letters, numbers, and underscores");
        }

        if (definition.getResource() == null || definition.getResource().isEmpty()) {
            result.addError("resource", "Resource is required");
        }

        if (definition.getOperation() == null || definition.getOperation().isEmpty()) {
            result.addError("operation", "Operation is required");
        }
    }

    private void validateApi(PermissionDefinition definition, ValidationResult result) {
        if (definition.getApi() == null) {
            result.addError("api", "API definition is required");
            return;
        }

        if (definition.getApi().getEndpoint() == null || definition.getApi().getEndpoint().isEmpty()) {
            result.addError("api.endpoint", "API endpoint is required");
        } else if (!(definition.getApi().getEndpoint().startsWith("/") || definition.getApi().getEndpoint().startsWith("*"))) {
            result.addError("api.endpoint", "API endpoint must start with /");
        }

        if (definition.getApi().getMethod() == null || definition.getApi().getMethod().isEmpty()) {
            result.addError("api.method", "HTTP method is required");
        } else if (!VALID_HTTP_METHODS.contains(definition.getApi().getMethod().toUpperCase())) {
            result.addError("api.method", "Invalid HTTP method");
        }
    }

    private void validateConfig(PermissionDefinition definition, ValidationResult result) {
        if (definition.getConfig() == null) {
            return; // Config is optional
        }

        // Validate branch access
        if (definition.getConfig().getBranchAccess() != null) {
            if (!VALID_BRANCH_ACCESS_TYPES.contains(definition.getConfig().getBranchAccess().getType())) {
                result.addError("config.branchAccess.type", "Invalid branch access type");
            }
            
            if ("SPECIFIC_BRANCHES".equals(definition.getConfig().getBranchAccess().getType()) 
                && (definition.getConfig().getBranchAccess().getAllowedBranches() == null 
                || definition.getConfig().getBranchAccess().getAllowedBranches().isEmpty())) {
                result.addError("config.branchAccess.allowedBranches", "Allowed branches must be specified for SPECIFIC_BRANCHES type");
            }
        }

        // Validate time access
        if (definition.getConfig().getTimeAccess() != null && definition.getConfig().getTimeAccess().isEnabled()) {
            if (definition.getConfig().getTimeAccess().getAllowedTimeWindows() == null 
                || definition.getConfig().getTimeAccess().getAllowedTimeWindows().isEmpty()) {
                result.addError("config.timeAccess.allowedTimeWindows", "Time windows must be specified when time access is enabled");
            } else {
                definition.getConfig().getTimeAccess().getAllowedTimeWindows().forEach(window -> {
                    if (!TIME_WINDOW_PATTERN.matcher(window.getStartTime()).matches()) {
                        result.addError("config.timeAccess.allowedTimeWindows", "Invalid start time format");
                    }
                    if (!TIME_WINDOW_PATTERN.matcher(window.getEndTime()).matches()) {
                        result.addError("config.timeAccess.allowedTimeWindows", "Invalid end time format");
                    }
                });
            }
        }

        // Validate amount limits
        if (definition.getConfig().getAmountLimit() != null && definition.getConfig().getAmountLimit().isEnabled()) {
            if (definition.getConfig().getAmountLimit().getDefaultLimit() == null) {
                result.addError("config.amountLimit.defaultLimit", "Default limit must be specified when amount limit is enabled");
            }
            
            if ("ROLE_BASED".equals(definition.getConfig().getAmountLimit().getLimitType()) 
                && (definition.getConfig().getAmountLimit().getRoleLimits() == null 
                || definition.getConfig().getAmountLimit().getRoleLimits().isEmpty())) {
                result.addError("config.amountLimit.roleLimits", "Role limits must be specified for ROLE_BASED limit type");
            }
        }

        // Validate validation rules
        if (definition.getConfig().getValidationRules() != null) {
            definition.getConfig().getValidationRules().forEach(rule -> {
                if (rule.getCondition() == null || rule.getCondition().isEmpty()) {
                    result.addError("config.validationRules", "Validation rule condition is required");
                }
                if (rule.getErrorMessage() == null || rule.getErrorMessage().isEmpty()) {
                    result.addError("config.validationRules", "Validation rule error message is required");
                }
            });
        }
    }
} 