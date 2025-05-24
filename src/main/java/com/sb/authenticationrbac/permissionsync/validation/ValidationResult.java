package com.sb.authenticationrbac.permissionsync.validation;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class ValidationResult {
    private boolean valid;
    private List<ValidationError> errors = new ArrayList<>();

    public static ValidationResult success() {
        ValidationResult result = new ValidationResult();
        result.setValid(true);
        return result;
    }

    public static ValidationResult failure(List<ValidationError> errors) {
        ValidationResult result = new ValidationResult();
        result.setValid(false);
        result.setErrors(errors);
        return result;
    }

    public void addError(String field, String message) {
        this.errors.add(new ValidationError(field, message));
        this.valid = false;
    }
} 