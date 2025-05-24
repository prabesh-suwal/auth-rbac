package com.sb.authenticationrbac.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleApprovalChainConfig {
    private boolean enabled;

    @Field("amount_threshold")
    private Double amountThreshold;
}
