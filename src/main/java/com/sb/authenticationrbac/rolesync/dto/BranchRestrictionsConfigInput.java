package com.sb.authenticationrbac.rolesync.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BranchRestrictionsConfigInput {
    private String type;
    private boolean allowCrossBranchView; // Optional, so provide a default or handle null
}
