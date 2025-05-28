package com.sb.authenticationrbac.dtos.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
public class UserPermissionConfig {
    @Valid
    private BranchAccessOverride branchAccessOverride;

    private Map<String, Double> amountLimitOverrides;

    @Valid
    private List<ResourceAccess> resourceAccesses;

    @Valid
    private List<TemporaryPermission> temporaryPermissions;

    private List<String> customConditions;

    @Data
    public static class BranchAccessOverride {
        @NotBlank(message = "Branch access type is required")
        private String type;
        
        private List<String> allowedBranches;
        private boolean includeSubBranches;
    }

    @Data
    public static class ResourceAccess {
        @NotBlank(message = "Resource type is required")
        private String resourceType;

        @NotBlank(message = "Resource ID is required")
        private String resourceId;

        @NotBlank(message = "Access type is required")
        private String accessType;

        @NotEmpty(message = "At least one allowed operation is required")
        private List<String> allowedOperations;

        private Instant expiresAt;
    }

    @Data
    public static class TemporaryPermission {
        @NotBlank(message = "Permission ID is required")
        private String permissionId;

        @NotNull(message = "Grant time is required")
        private Instant grantedAt;

        @NotNull(message = "Expiry time is required")
        private Instant expiresAt;

        @NotBlank(message = "Granter ID is required")
        private String grantedBy;

        private String reason;

        private Map<String, Object> conditions;
    }
} 