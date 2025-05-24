package com.sb.authenticationrbac.entities;

import lombok.Data;

import java.util.List;

@Data

public class DelegationConfig {
    private boolean enabled = false;
    private List<String> delegatablePermissions;
    private int maxDelegationDepth = 1;
    private long delegationDurationHours = 24;
    
    // constructors, getters, setters
}