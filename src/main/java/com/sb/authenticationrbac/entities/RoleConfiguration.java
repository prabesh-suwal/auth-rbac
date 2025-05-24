package com.sb.authenticationrbac.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleConfiguration {

    @Field("default_amount_limits")
    private Map<String, Double> defaultAmountLimits;

    @Field("working_hours")
    private RoleWorkingHoursConfig workingHours;

    @Field("branch_restrictions")
    private RoleBranchRestrictionsConfig branchRestrictions;

    @Field("approval_chain")
    private Map<String, RoleApprovalChainConfig> approvalChain;

    @Field("audit_features")
    private RoleAuditFeaturesConfig auditFeatures;

    @Field("risk_assessment_features")
    private RoleRiskAssessmentFeaturesConfig riskAssessmentFeatures;
}
