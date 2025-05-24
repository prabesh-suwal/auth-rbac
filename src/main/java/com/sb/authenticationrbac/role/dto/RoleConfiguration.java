package com.sb.authenticationrbac.role.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class RoleConfiguration {
    @JsonProperty("defaultAmountLimits")
    private Map<String, @DecimalMin(value = "0.0", message = "Amount limit must be positive") Double> defaultAmountLimits;
    
    @Valid
    @JsonProperty("workingHours")
    private WorkingHours workingHours;
    
    @Valid
    @JsonProperty("branchRestrictions")
    private BranchRestrictions branchRestrictions;
    
    @Valid
    @JsonProperty("approvalChain")
    private ApprovalChain approvalChain;
    
    @Valid
    @JsonProperty("auditFeatures")
    private AuditFeatures auditFeatures;
    
    @Valid
    @JsonProperty("riskAssessmentFeatures")
    private RiskAssessmentFeatures riskAssessmentFeatures;
    
    @Data
    public static class WorkingHours {
        private boolean enabled;
        
        @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Start time must be in HH:mm format")
        private String startTime;
        
        @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "End time must be in HH:mm format")
        private String endTime;
        
        @NotEmpty(message = "Working days cannot be empty")
        @Size(min = 1, message = "At least one working day must be specified")
        private List<@Pattern(regexp = "^(SUNDAY|MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY)$", 
                            message = "Invalid day format") String> workingDays;
    }
    
    @Data
    public static class BranchRestrictions {
        @NotNull(message = "Branch restriction type is required")
        @Pattern(regexp = "^(OWN_BRANCH_ONLY|OWN_BRANCH_AND_SUBORDINATES|ALL_BRANCHES)$", 
                message = "Invalid branch restriction type")
        private String type;
        
        private boolean allowCrossBranchView;
    }
    
    @Data
    public static class ApprovalChain {
        @Valid
        @JsonProperty("requiresBranchManagerApproval")
        private ApprovalThreshold branchManagerApproval;
        
        @Valid
        @JsonProperty("requiresCreditCommitteeApproval")
        private ApprovalThreshold creditCommitteeApproval;
    }
    
    @Data
    public static class ApprovalThreshold {
        private boolean enabled;
        
        @DecimalMin(value = "0.0", message = "Amount threshold must be positive")
        private double amountThreshold;
    }
    
    @Data
    public static class AuditFeatures {
        private boolean canViewDeletedRecords;
        private boolean canViewAuditHistory;
    }
    
    @Data
    public static class RiskAssessmentFeatures {
        private boolean canOverrideRiskScores;
        private boolean canModifyRiskParameters;
    }
} 