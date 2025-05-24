package com.sb.authenticationrbac.validator.role;

import com.sb.authenticationrbac.exception.ValidationException;
import com.sb.authenticationrbac.role.dto.RoleDefinition;
import com.sb.authenticationrbac.role.dto.RoleConfiguration;
import com.sb.authenticationrbac.validator.Validator;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class RoleConfigurationValidator implements Validator<RoleDefinition> {
    private static final Pattern TIME_PATTERN = Pattern.compile("^([01]?[0-9]|2[0-3]):[0-5][0-9]$");
    private static final List<String> VALID_DAYS = List.of(
        "SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"
    );
    private static final List<String> VALID_BRANCH_RESTRICTIONS = List.of(
        "OWN_BRANCH_ONLY", "OWN_BRANCH_AND_SUBORDINATES", "ALL_BRANCHES"
    );

    @Override
    public void validate(RoleDefinition role) throws ValidationException {
        RoleConfiguration config = role.getConfiguration();
        if (config == null) {
            throw new ValidationException("Role configuration is required");
        }

        validateWorkingHours(config);
        validateBranchRestrictions(config);
        validateAmountLimits(config);
    }

    private void validateWorkingHours(RoleConfiguration config) {
        var workingHours = config.getWorkingHours();
        if (workingHours == null) {
            return; // Working hours are optional
        }

        if (workingHours.isEnabled()) {
            if (workingHours.getStartTime() != null && !TIME_PATTERN.matcher(workingHours.getStartTime()).matches()) {
                throw new ValidationException("Start time must be in HH:mm format");
            }
            if (workingHours.getEndTime() != null && !TIME_PATTERN.matcher(workingHours.getEndTime()).matches()) {
                throw new ValidationException("End time must be in HH:mm format");
            }
            if (workingHours.getWorkingDays() != null) {
                for (String day : workingHours.getWorkingDays()) {
                    if (!VALID_DAYS.contains(day)) {
                        throw new ValidationException("Invalid working day: " + day);
                    }
                }
            }
        }
    }

    private void validateBranchRestrictions(RoleConfiguration config) {
        var restrictions = config.getBranchRestrictions();
        if (restrictions == null) {
            return; // Branch restrictions are optional
        }

        String type = restrictions.getType();
        if (type != null && !VALID_BRANCH_RESTRICTIONS.contains(type)) {
            throw new ValidationException("Invalid branch restriction type: " + type);
        }
    }

    private void validateAmountLimits(RoleConfiguration config) {
        var limits = config.getDefaultAmountLimits();
        if (limits == null) {
            return; // Amount limits are optional
        }

        for (var entry : limits.entrySet()) {
            if (entry.getValue() != null && entry.getValue() < 0) {
                throw new ValidationException("Amount limit must be positive for: " + entry.getKey());
            }
        }
    }
} 