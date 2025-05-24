package com.sb.authenticationrbac.entities;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class UserPermissionConfig {
    // User-specific branch access overrides
    private BranchAccessConfig branchAccessOverride;
    
    // User-specific amount limits
    private Map<String, Double> amountLimitOverrides; // Permission -> Limit
    
    // Specific resource access grants/denies
    private List<ResourceAccess> resourceAccesses;
    
    // Time-based overrides
    private TimeAccessConfig timeAccessOverride;
    
    // Custom user conditions
    private List<String> customConditions;
    
    // Temporary permission grants
    private List<TemporaryPermission> temporaryPermissions;
    
    // constructors, getters, setters
}