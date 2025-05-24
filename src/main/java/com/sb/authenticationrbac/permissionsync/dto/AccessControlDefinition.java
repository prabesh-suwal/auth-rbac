package com.sb.authenticationrbac.permissionsync.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sb.authenticationrbac.entities.*;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AccessControlDefinition {
    @JsonProperty("required_role")
    private String requiredRole;
    
    @JsonProperty("branch_access")
    private String branchAccess;
    
    @JsonProperty("time_window")
    private String timeWindow;
    
    @JsonProperty("amount_limit")
    private Double amountLimit;
    
    @JsonProperty("validation_rules")
    private List<String> validationRules;
    
    @JsonProperty("additional_config")
    private Map<String, Object> additionalConfig;
    
    public PermissionConfig toPermissionConfig() {
        PermissionConfig config = new PermissionConfig();
        
        // Branch access configuration
        BranchAccessConfig branchConfig = new BranchAccessConfig();
        branchConfig.setType(this.branchAccess);
        config.setBranchAccess(branchConfig);
        
        // Amount limit configuration
        if (this.amountLimit != null) {
            AmountLimitConfig amountConfig = new AmountLimitConfig();
            amountConfig.setEnabled(true);
            amountConfig.setDefaultLimit(this.amountLimit);
            config.setAmountLimit(amountConfig);
        }
        
        // Time access configuration
        TimeAccessConfig timeConfig = new TimeAccessConfig();
        timeConfig.setEnabled(true);
        TimeWindow window = new TimeWindow();
        if ("24x7".equals(this.timeWindow)) {
            window.setStartTime("00:00");
            window.setEndTime("23:59");
        } else {
            String[] parts = this.timeWindow.split("-");
            if (parts.length == 2) {
                window.setStartTime(parts[0]);
                window.setEndTime(parts[1]);
            }
        }
        timeConfig.setAllowedTimeWindows(List.of(window));
        config.setTimeAccess(timeConfig);
        
        // Validation rules
        if (this.validationRules != null) {
            List<ValidationRule> rules = this.validationRules.stream()
                .map(rule -> {
                    ValidationRule validationRule = new ValidationRule();
                    validationRule.setCondition(rule);
                    return validationRule;
                })
                .toList();
            config.setValidationRules(rules);
        }
        
        return config;
    }
} 