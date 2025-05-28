package com.sb.authenticationrbac.services;

import com.sb.authenticationrbac.dtos.PermissionResult;
import com.sb.authenticationrbac.entities.*;
import com.sb.authenticationrbac.repositories.RoleRepository;
import com.sb.authenticationrbac.repositories.UserRepository;
import com.sb.authenticationrbac.role.dto.RoleConfiguration;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service to manage and demonstrate the Branch Access Hierarchy System
 * 
 * Hierarchy (highest to lowest priority):
 * 1. User-specific overrides
 * 2. Role-level configuration  
 * 3. Permission-level configuration
 */
@Service
public class BranchAccessHierarchyService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DynamicPermissionEvaluationService permissionEvaluationService;

    public BranchAccessHierarchyService(UserRepository userRepository, 
                                       RoleRepository roleRepository,
                                       DynamicPermissionEvaluationService permissionEvaluationService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionEvaluationService = permissionEvaluationService;
    }

    /**
     * Test the complete hierarchy for a specific user and branch access scenario
     */
    public BranchAccessAnalysis analyzeBranchAccess(String userId, String targetBranchId, 
                                                   String permissionName, Object resource) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return new BranchAccessAnalysis(false, "User not found", Collections.emptyList());
        }

        List<String> evaluationSteps = new ArrayList<>();
        String userBranchId = user.getBranchId();
        
        // Step 1: Check user-specific overrides
        BranchAccessStep userStep = checkUserSpecificAccess(user, userBranchId, targetBranchId);
        evaluationSteps.add("STEP 1 - User-specific overrides: " + userStep.getDescription());
        
        if (userStep.isDecisive()) {
            return new BranchAccessAnalysis(userStep.isAllowed(), userStep.getReason(), evaluationSteps);
        }

        // Step 2: Check role-level configuration
        BranchAccessStep roleStep = checkRoleLevelAccess(user, userBranchId, targetBranchId);
        evaluationSteps.add("STEP 2 - Role-level configuration: " + roleStep.getDescription());
        
        if (roleStep.isDecisive()) {
            return new BranchAccessAnalysis(roleStep.isAllowed(), roleStep.getReason(), evaluationSteps);
        }

        // Step 3: Default to permission-level (would be handled by permission evaluation)
        evaluationSteps.add("STEP 3 - Permission-level: Will be evaluated by permission configuration");
        
        return new BranchAccessAnalysis(false, "Requires permission-level evaluation", evaluationSteps);
    }

    /**
     * Configure user-specific branch access override
     */
    public void configureUserBranchAccess(String userId, BranchAccessConfig branchAccessConfig) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        if (user.getPermissionConfig() == null) {
            user.setPermissionConfig(new UserPermissionConfig());
        }

        user.getPermissionConfig().setBranchAccessOverride(branchAccessConfig);
        userRepository.save(user);
    }

    /**
     * Configure role-level branch access
     */
    public void configureRoleBranchAccess(String roleId, String branchRestrictionType, boolean allowCrossBranchView) {
        Role role = roleRepository.findById(roleId).orElse(null);
        if (role == null) {
            throw new IllegalArgumentException("Role not found: " + roleId);
        }

        if (role.getConfiguration() == null) {
            role.setConfiguration(new RoleConfiguration());
        }

        RoleConfiguration.BranchRestrictions branchRestrictions = new RoleConfiguration.BranchRestrictions();
        branchRestrictions.setType(branchRestrictionType);
        branchRestrictions.setAllowCrossBranchView(allowCrossBranchView);
        
        role.getConfiguration().setBranchRestrictions(branchRestrictions);
        roleRepository.save(role);
    }

    /**
     * Get comprehensive branch access summary for a user
     */
    public UserBranchAccessSummary getUserBranchAccessSummary(String userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }

        UserBranchAccessSummary summary = new UserBranchAccessSummary();
        summary.setUserId(userId);
        summary.setUsername(user.getUsername());
        summary.setUserBranchId(user.getBranchId());

        // User-specific overrides
        if (user.getPermissionConfig() != null && user.getPermissionConfig().getBranchAccessOverride() != null) {
            summary.setUserSpecificOverride(user.getPermissionConfig().getBranchAccessOverride());
        }

        // Role-level configurations
        List<Role> userRoles = roleRepository.findByIdInAndActive(user.getRoleIds(), true);
        Map<String, RoleConfiguration.BranchRestrictions> roleBranchConfigs = new HashMap<>();
        
        for (Role role : userRoles) {
            if (role.getConfiguration() != null && role.getConfiguration().getBranchRestrictions() != null) {
                roleBranchConfigs.put(role.getName(), role.getConfiguration().getBranchRestrictions());
            }
        }
        summary.setRoleBranchConfigurations(roleBranchConfigs);

        return summary;
    }

    /**
     * Demonstrate cross-branch scenario (like RM2 creating loan for Branch 1)
     */
    public CrossBranchScenarioResult demonstrateCrossBranchScenario(String rmUserId, String targetBranchId, 
                                                                   String permissionName) {
        User rmUser = userRepository.findById(rmUserId).orElse(null);
        if (rmUser == null) {
            return new CrossBranchScenarioResult(false, "RM user not found", Collections.emptyList());
        }

        List<String> scenarioSteps = new ArrayList<>();
        scenarioSteps.add("Scenario: RM from branch " + rmUser.getBranchId() + 
                         " attempting to access branch " + targetBranchId);

        // Create a mock loan resource for the target branch
        Map<String, Object> mockLoan = new HashMap<>();
        Map<String, Object> mockBranch = new HashMap<>();
        mockBranch.put("id", targetBranchId);
        mockLoan.put("branch", mockBranch);

        // Analyze the access
        BranchAccessAnalysis analysis = analyzeBranchAccess(rmUserId, targetBranchId, permissionName, mockLoan);
        scenarioSteps.addAll(analysis.getEvaluationSteps());

        return new CrossBranchScenarioResult(analysis.isAccessGranted(), analysis.getFinalReason(), scenarioSteps);
    }

    // Helper methods for internal use
    private BranchAccessStep checkUserSpecificAccess(User user, String userBranchId, String targetBranchId) {
        if (user.getPermissionConfig() == null || user.getPermissionConfig().getBranchAccessOverride() == null) {
            return new BranchAccessStep(false, false, "No user-specific overrides configured");
        }

        BranchAccessConfig override = user.getPermissionConfig().getBranchAccessOverride();
        
        switch (override.getType()) {
            case "ALL_BRANCHES":
                return new BranchAccessStep(true, true, "User granted ALL_BRANCHES access");
            case "OWN_BRANCH":
                if (userBranchId.equals(targetBranchId)) {
                    return new BranchAccessStep(true, true, "User granted own branch access");
                } else {
                    return new BranchAccessStep(false, true, "User restricted to own branch only");
                }
            case "SPECIFIC_BRANCHES":
                if (override.getAllowedBranches() != null && override.getAllowedBranches().contains(targetBranchId)) {
                    return new BranchAccessStep(true, true, "User granted specific branch access");
                } else {
                    return new BranchAccessStep(false, true, "Target branch not in user's allowed list");
                }
            default:
                return new BranchAccessStep(false, false, "User override type not recognized, continuing to role level");
        }
    }

    private BranchAccessStep checkRoleLevelAccess(User user, String userBranchId, String targetBranchId) {
        List<Role> userRoles = roleRepository.findByIdInAndActive(user.getRoleIds(), true);
        
        boolean hasAllBranchesAccess = false;
        boolean hasSubordinateAccess = false;
        boolean hasCrossBranchView = false;
        boolean hasOwnBranchAccess = false;
        String highestPrivilegeRole = null;

        for (Role role : userRoles) {
            if (role.getConfiguration() == null || role.getConfiguration().getBranchRestrictions() == null) {
                continue;
            }

            RoleConfiguration.BranchRestrictions restrictions = role.getConfiguration().getBranchRestrictions();
            String restrictionType = restrictions.getType();
            boolean allowCrossBranch = restrictions.isAllowCrossBranchView();

            switch (restrictionType) {
                case "ALL_BRANCHES":
                    hasAllBranchesAccess = true;
                    highestPrivilegeRole = role.getName();
                    break;
                case "OWN_BRANCH_AND_SUBORDINATES":
                    if (!hasAllBranchesAccess) {
                        hasSubordinateAccess = true;
                        if (allowCrossBranch) {
                            hasCrossBranchView = true;
                        }
                        if (highestPrivilegeRole == null) {
                            highestPrivilegeRole = role.getName();
                        }
                    }
                    break;
                case "OWN_BRANCH_ONLY":
                    if (!hasAllBranchesAccess && !hasSubordinateAccess) {
                        hasOwnBranchAccess = true;
                        if (allowCrossBranch) {
                            hasCrossBranchView = true;
                        }
                        if (highestPrivilegeRole == null) {
                            highestPrivilegeRole = role.getName();
                        }
                    }
                    break;
            }
        }

        // Determine access based on highest privilege
        if (hasAllBranchesAccess) {
            return new BranchAccessStep(true, true, 
                String.format("Role %s grants ALL_BRANCHES access", highestPrivilegeRole));
        }

        if (hasSubordinateAccess && hasCrossBranchView) {
            return new BranchAccessStep(true, true,
                String.format("Role %s grants cross-branch access", highestPrivilegeRole));
        }

        // Handle own branch access
        if (hasOwnBranchAccess) {
            if (userBranchId.equals(targetBranchId)) {
                return new BranchAccessStep(true, true,
                    String.format("Role %s grants own branch access", highestPrivilegeRole));
            } else if (hasCrossBranchView) {
                return new BranchAccessStep(true, true,
                    String.format("Role %s grants cross-branch view access", highestPrivilegeRole));
            }
        }

        // Handle cross-branch view for other role types
        if (hasCrossBranchView && !userBranchId.equals(targetBranchId)) {
            return new BranchAccessStep(true, true,
                String.format("Role %s grants cross-branch view access", highestPrivilegeRole));
        }

        return new BranchAccessStep(false, false, "No role-level access granted, continuing to permission level");
    }

    // Data classes for results
    public static class BranchAccessAnalysis {
        private final boolean accessGranted;
        private final String finalReason;
        private final List<String> evaluationSteps;

        public BranchAccessAnalysis(boolean accessGranted, String finalReason, List<String> evaluationSteps) {
            this.accessGranted = accessGranted;
            this.finalReason = finalReason;
            this.evaluationSteps = evaluationSteps;
        }

        public boolean isAccessGranted() { return accessGranted; }
        public String getFinalReason() { return finalReason; }
        public List<String> getEvaluationSteps() { return evaluationSteps; }
    }

    public static class BranchAccessStep {
        private final boolean allowed;
        private final boolean decisive;
        private final String description;

        public BranchAccessStep(boolean allowed, boolean decisive, String description) {
            this.allowed = allowed;
            this.decisive = decisive;
            this.description = description;
        }

        public boolean isAllowed() { return allowed; }
        public boolean isDecisive() { return decisive; }
        public String getDescription() { return description; }
        public String getReason() { return description; }
    }

    public static class UserBranchAccessSummary {
        private String userId;
        private String username;
        private String userBranchId;
        private BranchAccessConfig userSpecificOverride;
        private Map<String, RoleConfiguration.BranchRestrictions> roleBranchConfigurations;

        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getUserBranchId() { return userBranchId; }
        public void setUserBranchId(String userBranchId) { this.userBranchId = userBranchId; }
        public BranchAccessConfig getUserSpecificOverride() { return userSpecificOverride; }
        public void setUserSpecificOverride(BranchAccessConfig userSpecificOverride) { this.userSpecificOverride = userSpecificOverride; }
        public Map<String, RoleConfiguration.BranchRestrictions> getRoleBranchConfigurations() { return roleBranchConfigurations; }
        public void setRoleBranchConfigurations(Map<String, RoleConfiguration.BranchRestrictions> roleBranchConfigurations) { this.roleBranchConfigurations = roleBranchConfigurations; }
    }

    public static class CrossBranchScenarioResult {
        private final boolean success;
        private final String reason;
        private final List<String> scenarioSteps;

        public CrossBranchScenarioResult(boolean success, String reason, List<String> scenarioSteps) {
            this.success = success;
            this.reason = reason;
            this.scenarioSteps = scenarioSteps;
        }

        public boolean isSuccess() { return success; }
        public String getReason() { return reason; }
        public List<String> getScenarioSteps() { return scenarioSteps; }
    }
} 