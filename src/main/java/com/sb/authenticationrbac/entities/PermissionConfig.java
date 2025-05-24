package com.sb.authenticationrbac.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class PermissionConfig {
    // Branch access configuration
    @JsonProperty("branch_access")
    private BranchAccessConfig branchAccess;
    
    // Amount limit configuration
    @JsonProperty("amount_limit")
    private AmountLimitConfig amountLimit;
    
    // Time-based access configuration
    @JsonProperty("time_access")
    private TimeAccessConfig timeAccess;
    
    // Resource-specific configuration
    @JsonProperty("resource_access")
    private ResourceAccessConfig resourceAccess;
    
    // Custom conditions (SpEL expressions)
    private List<String> conditions;
    
    // Custom validation rules
    @JsonProperty("validation_rules")
    private List<ValidationRule> validationRules;
    
    // Delegation rules
    private DelegationConfig delegation;
    
    // constructors, getters, setters
}