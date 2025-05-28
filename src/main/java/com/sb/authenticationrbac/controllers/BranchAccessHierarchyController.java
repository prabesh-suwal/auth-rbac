package com.sb.authenticationrbac.controllers;

import com.sb.authenticationrbac.aop.CheckPermission;
import com.sb.authenticationrbac.dtos.responses.MessageResponse;
import com.sb.authenticationrbac.entities.BranchAccessConfig;
import com.sb.authenticationrbac.services.BranchAccessHierarchyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller to demonstrate and test the Branch Access Hierarchy System
 * 
 * Provides endpoints to:
 * - Test cross-branch scenarios
 * - Configure user and role-level branch access
 * - Analyze branch access hierarchy
 */
@RestController
@RequestMapping("/api/admin/branch-access-hierarchy")
public class BranchAccessHierarchyController {

    private final BranchAccessHierarchyService branchAccessHierarchyService;

    public BranchAccessHierarchyController(BranchAccessHierarchyService branchAccessHierarchyService) {
        this.branchAccessHierarchyService = branchAccessHierarchyService;
    }

    /**
     * Demonstrate the cross-branch scenario (RM2 creating loan for Branch 1)
     */
    @GetMapping("/demo/cross-branch/{rmUserId}/{targetBranchId}")
    @CheckPermission(value = "SYSTEM_MONITOR", operation = "READ")
    public ResponseEntity<?> demonstrateCrossBranchScenario(
            @PathVariable String rmUserId,
            @PathVariable String targetBranchId,
            @RequestParam(defaultValue = "LOAN_CREATE") String permissionName) {
        
        try {
            BranchAccessHierarchyService.CrossBranchScenarioResult result = 
                branchAccessHierarchyService.demonstrateCrossBranchScenario(rmUserId, targetBranchId, permissionName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("scenario", "Cross-Branch Access Test");
            response.put("rmUserId", rmUserId);
            response.put("targetBranchId", targetBranchId);
            response.put("permissionName", permissionName);
            response.put("accessGranted", result.isSuccess());
            response.put("reason", result.getReason());
            response.put("evaluationSteps", result.getScenarioSteps());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error demonstrating cross-branch scenario: " + e.getMessage()));
        }
    }

    /**
     * Analyze branch access hierarchy for a specific user and target branch
     */
    @GetMapping("/analyze/{userId}/{targetBranchId}")
    @CheckPermission(value = "SYSTEM_MONITOR", operation = "READ")
    public ResponseEntity<?> analyzeBranchAccess(
            @PathVariable String userId,
            @PathVariable String targetBranchId,
            @RequestParam(defaultValue = "LOAN_CREATE") String permissionName) {
        
        try {
            BranchAccessHierarchyService.BranchAccessAnalysis analysis = 
                branchAccessHierarchyService.analyzeBranchAccess(userId, targetBranchId, permissionName, null);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("targetBranchId", targetBranchId);
            response.put("permissionName", permissionName);
            response.put("accessGranted", analysis.isAccessGranted());
            response.put("finalReason", analysis.getFinalReason());
            response.put("hierarchyEvaluation", analysis.getEvaluationSteps());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error analyzing branch access: " + e.getMessage()));
        }
    }

    /**
     * Get comprehensive branch access summary for a user
     */
    @GetMapping("/summary/{userId}")
    @CheckPermission(value = "USER_VIEW", operation = "READ")
    public ResponseEntity<?> getUserBranchAccessSummary(@PathVariable String userId) {
        try {
            BranchAccessHierarchyService.UserBranchAccessSummary summary = 
                branchAccessHierarchyService.getUserBranchAccessSummary(userId);
            
            if (summary == null) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("User not found: " + userId));
            }
            
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error getting user branch access summary: " + e.getMessage()));
        }
    }

    /**
     * Configure user-specific branch access override
     */
    @PostMapping("/configure/user/{userId}/branch-access")
    @CheckPermission(value = "USER_UPDATE", operation = "UPDATE")
    public ResponseEntity<?> configureUserBranchAccess(
            @PathVariable String userId,
            @RequestBody BranchAccessConfigRequest request) {
        
        try {
            BranchAccessConfig config = new BranchAccessConfig();
            config.setType(request.getType());
            config.setAllowedBranches(request.getAllowedBranches());
            config.setBranchFieldPath(request.getBranchFieldPath());
            config.setIncludeSubBranches(request.isIncludeSubBranches());
            
            branchAccessHierarchyService.configureUserBranchAccess(userId, config);
            
            return ResponseEntity.ok(new MessageResponse(
                String.format("User %s branch access configured with type: %s", userId, request.getType())));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error configuring user branch access: " + e.getMessage()));
        }
    }

    /**
     * Configure role-level branch access
     */
    @PostMapping("/configure/role/{roleId}/branch-access")
    @CheckPermission(value = "ROLE_UPDATE", operation = "UPDATE")
    public ResponseEntity<?> configureRoleBranchAccess(
            @PathVariable String roleId,
            @RequestBody RoleBranchAccessRequest request) {
        
        try {
            branchAccessHierarchyService.configureRoleBranchAccess(
                roleId, 
                request.getBranchRestrictionType(), 
                request.isAllowCrossBranchView()
            );
            
            return ResponseEntity.ok(new MessageResponse(
                String.format("Role %s branch access configured: %s (cross-branch: %s)", 
                    roleId, request.getBranchRestrictionType(), request.isAllowCrossBranchView())));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error configuring role branch access: " + e.getMessage()));
        }
    }

    /**
     * Test the complete hierarchy with a real-world scenario
     */
    @PostMapping("/test/scenario")
    @CheckPermission(value = "SYSTEM_MONITOR", operation = "READ")
    public ResponseEntity<?> testHierarchyScenario(@RequestBody HierarchyTestRequest request) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("testScenario", request.getScenarioName());
            response.put("description", request.getDescription());
            
            // Test each user in the scenario
            Map<String, Object> userResults = new HashMap<>();
            for (String userId : request.getUserIds()) {
                BranchAccessHierarchyService.BranchAccessAnalysis analysis = 
                    branchAccessHierarchyService.analyzeBranchAccess(
                        userId, 
                        request.getTargetBranchId(), 
                        request.getPermissionName(), 
                        null
                    );
                
                Map<String, Object> userResult = new HashMap<>();
                userResult.put("accessGranted", analysis.isAccessGranted());
                userResult.put("reason", analysis.getFinalReason());
                userResult.put("evaluationSteps", analysis.getEvaluationSteps());
                
                userResults.put(userId, userResult);
            }
            
            response.put("userResults", userResults);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error testing hierarchy scenario: " + e.getMessage()));
        }
    }

    /**
     * Get hierarchy documentation and examples
     */
    @GetMapping("/documentation")
    public ResponseEntity<?> getHierarchyDocumentation() {
        Map<String, Object> documentation = new HashMap<>();
        
        documentation.put("title", "Branch Access Hierarchy System");
        documentation.put("description", "Three-tier access control system for branch-based permissions");
        
        Map<String, Object> hierarchy = new HashMap<>();
        hierarchy.put("level1", Map.of(
            "name", "User-specific overrides",
            "priority", "Highest",
            "description", "Individual user configurations that override all other settings",
            "examples", List.of(
                "User granted ALL_BRANCHES access",
                "User restricted to specific branches only",
                "User denied access to certain branches"
            )
        ));
        
        hierarchy.put("level2", Map.of(
            "name", "Role-level configuration",
            "priority", "Medium",
            "description", "Role-based branch access rules that can override permission restrictions",
            "examples", List.of(
                "RELATIONSHIP_MANAGER role with cross-branch access",
                "BRANCH_MANAGER role with subordinate branch access",
                "ADMIN role with all branches access"
            )
        ));
        
        hierarchy.put("level3", Map.of(
            "name", "Permission-level configuration",
            "priority", "Lowest",
            "description", "Default permission-based access control",
            "examples", List.of(
                "LOAN_CREATE permission restricted to OWN_BRANCH",
                "USER_VIEW permission with SPECIFIC_BRANCHES",
                "REPORT_VIEW permission with BRANCH_HIERARCHY"
            )
        ));
        
        documentation.put("hierarchy", hierarchy);
        
        Map<String, Object> crossBranchExample = new HashMap<>();
        crossBranchExample.put("scenario", "RM2 (Branch 2) creating loan for Branch 1");
        crossBranchExample.put("steps", List.of(
            "1. Check if RM2 has user-specific branch override",
            "2. Check if RELATIONSHIP_MANAGER role allows cross-branch access",
            "3. Apply LOAN_CREATE permission branch restrictions",
            "4. Grant or deny access based on highest priority rule"
        ));
        
        documentation.put("crossBranchExample", crossBranchExample);
        
        return ResponseEntity.ok(documentation);
    }

    // Request DTOs
    public static class BranchAccessConfigRequest {
        private String type;
        private List<String> allowedBranches;
        private String branchFieldPath;
        private boolean includeSubBranches;

        // Getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public List<String> getAllowedBranches() { return allowedBranches; }
        public void setAllowedBranches(List<String> allowedBranches) { this.allowedBranches = allowedBranches; }
        public String getBranchFieldPath() { return branchFieldPath; }
        public void setBranchFieldPath(String branchFieldPath) { this.branchFieldPath = branchFieldPath; }
        public boolean isIncludeSubBranches() { return includeSubBranches; }
        public void setIncludeSubBranches(boolean includeSubBranches) { this.includeSubBranches = includeSubBranches; }
    }

    public static class RoleBranchAccessRequest {
        private String branchRestrictionType;
        private boolean allowCrossBranchView;

        // Getters and setters
        public String getBranchRestrictionType() { return branchRestrictionType; }
        public void setBranchRestrictionType(String branchRestrictionType) { this.branchRestrictionType = branchRestrictionType; }
        public boolean isAllowCrossBranchView() { return allowCrossBranchView; }
        public void setAllowCrossBranchView(boolean allowCrossBranchView) { this.allowCrossBranchView = allowCrossBranchView; }
    }

    public static class HierarchyTestRequest {
        private String scenarioName;
        private String description;
        private List<String> userIds;
        private String targetBranchId;
        private String permissionName;

        // Getters and setters
        public String getScenarioName() { return scenarioName; }
        public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<String> getUserIds() { return userIds; }
        public void setUserIds(List<String> userIds) { this.userIds = userIds; }
        public String getTargetBranchId() { return targetBranchId; }
        public void setTargetBranchId(String targetBranchId) { this.targetBranchId = targetBranchId; }
        public String getPermissionName() { return permissionName; }
        public void setPermissionName(String permissionName) { this.permissionName = permissionName; }
    }
} 