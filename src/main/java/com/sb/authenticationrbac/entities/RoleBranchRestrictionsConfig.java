package com.sb.authenticationrbac.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleBranchRestrictionsConfig {
    private String type;

    @Field("allow_cross_branch_view")
    private boolean allowCrossBranchView;
}
