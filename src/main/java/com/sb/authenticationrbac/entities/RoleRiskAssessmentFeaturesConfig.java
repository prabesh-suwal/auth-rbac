package com.sb.authenticationrbac.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleRiskAssessmentFeaturesConfig {
    @Field("can_override_risk_scores")
    private boolean canOverrideRiskScores;

    @Field("can_modify_risk_parameters")
    private boolean canModifyRiskParameters;
}
