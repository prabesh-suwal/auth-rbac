package com.sb.authenticationrbac.entities;

import lombok.Data;
import java.util.List;

@Data
public class PermissionConfig {
    // Branch access configuration
    private BranchAccessConfig branchAccess;
    
    // Amount limit configuration
    private AmountLimitConfig amountLimit;
    
    // Time-based access configuration
    private TimeAccessConfig timeAccess;
    
    // Resource-specific configuration
    private ResourceAccessConfig resourceAccess;
    
    // Custom conditions (SpEL expressions)
    private List<String> conditions;
    
    // Custom validation rules
    private List<ValidationRule> validationRules;
    
    // Delegation rules
    private DelegationConfig delegation;
    
    // constructors, getters, setters
}