//package com.sb.authenticationrbac.validator.role;
//
//import com.sb.authenticationrbac.exception.ValidationException;
//import com.sb.authenticationrbac.repositories.PermissionRepository;
//import com.sb.authenticationrbac.role.dto.RoleConfiguration;
//import com.sb.authenticationrbac.role.dto.RoleDefinition;
//import com.sb.authenticationrbac.validator.AbstractValidator;
//import com.sb.authenticationrbac.validator.unique.UniqueValidator;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//import java.util.regex.Pattern;
//@UniqueValidator
//@Component
//public class CreateRoleValidator1 extends AbstractValidator<RoleDefinition> {
//    private static final String NAME_PATTERN = "^[A-Z][A-Z0-9_]*$";
//    private static final Pattern TIME_PATTERN = Pattern.compile("^([01]?[0-9]|2[0-3]):[0-5][0-9]$");
//    private static final List<String> VALID_DAYS = List.of(
//        "SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"
//    );
//    private static final List<String> VALID_BRANCH_RESTRICTIONS = List.of(
//        "OWN_BRANCH_ONLY", "OWN_BRANCH_AND_SUBORDINATES", "ALL_BRANCHES"
//    );
//
//    private final PermissionRepository permissionRepository;
//
//    public CreateRoleValidator1(PermissionRepository permissionRepository) {
//        this.permissionRepository = permissionRepository;
//    }
//
//    @Override
//    protected void preValidate(RoleDefinition role) throws ValidationException {
//        if (role == null) {
//            throw new ValidationException("Role definition cannot be null");
//        }
//    }
//
//    @Override
//    protected void doValidate(RoleDefinition role) throws ValidationException {
//        validateBasicInfo(role);
//        validatePermissions(role);
//        validateConfiguration(role);
//    }
//
//    @Override
//    protected void postValidate(RoleDefinition role) throws ValidationException {
//        // Add any post-validation logic if needed
//    }
//
//    private void validateBasicInfo(RoleDefinition role) {
//        String name = role.getName();
//        if (name == null || name.trim().isEmpty()) {
//            throw new ValidationException("Role name is required");
//        }
//
//        if (!name.matches(NAME_PATTERN)) {
//            throw new ValidationException("Role name must be uppercase with underscores and numbers only");
//        }
//
//        String description = role.getDescription();
//        if (description == null || description.trim().isEmpty()) {
//            throw new ValidationException("Role description is required");
//        }
//
//        if (description.length() < 10 || description.length() > 200) {
//            throw new ValidationException("Description must be between 10 and 200 characters");
//        }
//    }
//
//    private void validatePermissions(RoleDefinition role) {
//        var permissionIds = role.getPermissionIds();
//        if (permissionIds == null || permissionIds.isEmpty()) {
//            throw new ValidationException("At least one permission is required");
//        }
//
//        for (String permissionId : permissionIds) {
//            if (permissionId == null || permissionId.trim().isEmpty()) {
//                throw new ValidationException("Permission ID cannot be blank");
//            }
//
//            if (!permissionRepository.existsById(permissionId)) {
//                throw new ValidationException("Permission ID '" + permissionId + "' does not exist");
//            }
//        }
//    }
//
//    private void validateConfiguration(RoleDefinition role) {
//        RoleConfiguration config = role.getConfiguration();
//        if (config == null) {
//            throw new ValidationException("Role configuration is required");
//        }
//
//        validateWorkingHours(config);
//        validateBranchRestrictions(config);
//        validateAmountLimits(config);
//    }
//
//    private void validateWorkingHours(RoleConfiguration config) {
//        var workingHours = config.getWorkingHours();
//        if (workingHours == null) {
//            return; // Working hours are optional
//        }
//
//        if (workingHours.isEnabled()) {
//            if (workingHours.getStartTime() != null && !TIME_PATTERN.matcher(workingHours.getStartTime()).matches()) {
//                throw new ValidationException("Start time must be in HH:mm format");
//            }
//            if (workingHours.getEndTime() != null && !TIME_PATTERN.matcher(workingHours.getEndTime()).matches()) {
//                throw new ValidationException("End time must be in HH:mm format");
//            }
//            if (workingHours.getWorkingDays() != null) {
//                for (String day : workingHours.getWorkingDays()) {
//                    if (!VALID_DAYS.contains(day)) {
//                        throw new ValidationException("Invalid working day: " + day);
//                    }
//                }
//            }
//        }
//    }
//
//    private void validateBranchRestrictions(RoleConfiguration config) {
//        var restrictions = config.getBranchRestrictions();
//        if (restrictions == null) {
//            return; // Branch restrictions are optional
//        }
//
//        String type = restrictions.getType();
//        if (type != null && !VALID_BRANCH_RESTRICTIONS.contains(type)) {
//            throw new ValidationException("Invalid branch restriction type: " + type);
//        }
//    }
//
//    private void validateAmountLimits(RoleConfiguration config) {
//        var limits = config.getDefaultAmountLimits();
//        if (limits == null) {
//            return; // Amount limits are optional
//        }
//
//        for (var entry : limits.entrySet()) {
//            if (entry.getValue() != null && entry.getValue() < 0) {
//                throw new ValidationException("Amount limit must be positive for: " + entry.getKey());
//            }
//        }
//    }
//}