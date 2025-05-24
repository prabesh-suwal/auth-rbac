package com.sb.authenticationrbac.entities;

import lombok.Data;

import java.util.Map;


@Data
public class AmountLimitConfig {
    private boolean enabled = false;
    private String limitType; // "FIXED", "ROLE_BASED", "USER_SPECIFIC", "DYNAMIC"
    private Double defaultLimit;
    private String amountFieldPath = "amount"; // Path to amount field in resource
    private Map<String, Double> roleLimits; // Role-specific limits
    private String dynamicLimitExpression; // SpEL expression for dynamic limits
    
    // constructors, getters, setters
}