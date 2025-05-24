package com.sb.authenticationrbac.rolesync.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleConfigurationInput {
    private Map<String, Double> defaultAmountLimits;
    private WorkingHoursConfigInput workingHours;
    private BranchRestrictionsConfigInput branchRestrictions;
    private Map<String, ApprovalChainConfigInput> approvalChain;
    private AuditFeaturesConfigInput auditFeatures;
    private RiskAssessmentFeaturesConfigInput riskAssessmentFeatures;
}
