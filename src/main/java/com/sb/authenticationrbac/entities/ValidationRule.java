package com.sb.authenticationrbac.entities;

import lombok.Data;

@Data
public class ValidationRule {
    private String name;
    private String condition; // SpEL expression
    private String errorMessage;
    private int priority = 0;
    
    // constructors, getters, setters
}
