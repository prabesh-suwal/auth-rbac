package com.sb.authenticationrbac.rolesync.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskAssessmentFeaturesConfigInput {
    private boolean canOverrideRiskScores;
    private boolean canModifyRiskParameters;
}
