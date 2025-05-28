package com.sb.authenticationrbac.controllers;

import com.sb.authenticationrbac.dtos.responses.MessageResponse;
import com.sb.authenticationrbac.entities.User;
import com.sb.authenticationrbac.security.SecurityContextUtils;
import com.sb.authenticationrbac.security.UserPrincipal;
import com.sb.authenticationrbac.services.UserContextService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controller demonstrating how to use SecurityContextUtils
 * to access current user information in REST endpoints.
 */
@RestController
@RequestMapping("/api/user-context")
public class UserContextController {

    private final UserContextService userContextService;

    public UserContextController(UserContextService userContextService) {
        this.userContextService = userContextService;
    }

    /**
     * Get current user information using SecurityContextUtils directly
     */
    @GetMapping("/current-user")
    public ResponseEntity<?> getCurrentUser() {
        Optional<UserPrincipal> userPrincipal = SecurityContextUtils.getCurrentUserPrincipal();
        
        if (userPrincipal.isPresent()) {
            UserPrincipal user = userPrincipal.get();
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("email", user.getEmail());
            userInfo.put("branchId", user.getBranchId());
            userInfo.put("authorities", user.getAuthorities());
            userInfo.put("active", user.isActive());
            
            return ResponseEntity.ok(userInfo);
        }
        
        return ResponseEntity.badRequest()
                .body(new MessageResponse("No authenticated user found"));
    }

    /**
     * Get current user ID using SecurityContextUtils
     */
    @GetMapping("/current-user-id")
    public ResponseEntity<?> getCurrentUserId() {
        Optional<String> userId = SecurityContextUtils.getCurrentUserId();
        
        if (userId.isPresent()) {
            Map<String, String> response = new HashMap<>();
            response.put("userId", userId.get());
            return ResponseEntity.ok(response);
        }
        
        return ResponseEntity.badRequest()
                .body(new MessageResponse("No authenticated user found"));
    }

    /**
     * Get current user's branch information
     */
    @GetMapping("/current-user-branch")
    public ResponseEntity<?> getCurrentUserBranch() {
        Optional<String> branchId = SecurityContextUtils.getCurrentUserBranchId();
        
        if (branchId.isPresent()) {
            Map<String, String> response = new HashMap<>();
            response.put("branchId", branchId.get());
            return ResponseEntity.ok(response);
        }
        
        return ResponseEntity.badRequest()
                .body(new MessageResponse("No authenticated user or branch information found"));
    }

    /**
     * Check if current user has a specific permission
     */
    @GetMapping("/check-permission/{permission}")
    public ResponseEntity<?> checkPermission(@PathVariable String permission) {
        boolean hasPermission = SecurityContextUtils.hasAuthority(permission);
        
        Map<String, Object> response = new HashMap<>();
        response.put("permission", permission);
        response.put("hasPermission", hasPermission);
        response.put("userId", SecurityContextUtils.getCurrentUserId().orElse("unknown"));
        
        return ResponseEntity.ok(response);
    }

    /**
     * Check if current user has a specific role
     */
    @GetMapping("/check-role/{role}")
    public ResponseEntity<?> checkRole(@PathVariable String role) {
        boolean hasRole = SecurityContextUtils.hasRole(role);
        
        Map<String, Object> response = new HashMap<>();
        response.put("role", role);
        response.put("hasRole", hasRole);
        response.put("userId", SecurityContextUtils.getCurrentUserId().orElse("unknown"));
        
        return ResponseEntity.ok(response);
    }

    /**
     * Check if current user has any of the specified permissions
     */
    @PostMapping("/check-any-permissions")
    public ResponseEntity<?> checkAnyPermissions(@RequestBody String[] permissions) {
        boolean hasAnyPermission = SecurityContextUtils.hasAnyAuthority(permissions);
        
        Map<String, Object> response = new HashMap<>();
        response.put("permissions", permissions);
        response.put("hasAnyPermission", hasAnyPermission);
        response.put("userId", SecurityContextUtils.getCurrentUserId().orElse("unknown"));
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get current user's full User entity from database using service
     */
    @GetMapping("/current-user-full")
    public ResponseEntity<?> getCurrentUserFull() {
        Optional<User> user = userContextService.getCurrentUser();
        
        if (user.isPresent()) {
            return ResponseEntity.ok(user.get());
        }
        
        return ResponseEntity.badRequest()
                .body(new MessageResponse("No authenticated user found"));
    }

    /**
     * Check branch access using business logic
     */
    @GetMapping("/check-branch-access/{branchId}")
    public ResponseEntity<?> checkBranchAccess(@PathVariable String branchId) {
        boolean canAccess = userContextService.canAccessBranch(branchId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("branchId", branchId);
        response.put("canAccess", canAccess);
        response.put("currentUserBranch", SecurityContextUtils.getCurrentUserBranchId().orElse("unknown"));
        response.put("isAdmin", SecurityContextUtils.hasRole("ADMIN"));
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get audit information for current user
     */
    @GetMapping("/audit-info")
    public ResponseEntity<?> getAuditInfo() {
        String auditInfo = userContextService.getCurrentUserForAudit();
        
        Map<String, Object> response = new HashMap<>();
        response.put("auditInfo", auditInfo);
        response.put("timestamp", System.currentTimeMillis());
        response.put("isAuthenticated", SecurityContextUtils.isAuthenticated());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Example endpoint that requires authentication (throws exception if not authenticated)
     */
    @GetMapping("/require-auth")
    public ResponseEntity<?> requireAuth() {
        try {
            String userInfo = userContextService.getRequiredUserInfo();
            return ResponseEntity.ok(new MessageResponse(userInfo));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Authentication required: " + e.getMessage()));
        }
    }

    /**
     * Check user management capabilities
     */
    @GetMapping("/can-manage-users")
    public ResponseEntity<?> canManageUsers() {
        boolean canManage = userContextService.canManageUsers();
        
        Map<String, Object> response = new HashMap<>();
        response.put("canManageUsers", canManage);
        response.put("userId", SecurityContextUtils.getCurrentUserId().orElse("unknown"));
        response.put("authorities", SecurityContextUtils.getCurrentUserPrincipal()
                .map(user -> user.getAuthorities().toString())
                .orElse("none"));
        
        return ResponseEntity.ok(response);
    }

    /**
     * Check if user is admin or manager
     */
    @GetMapping("/is-admin-or-manager")
    public ResponseEntity<?> isAdminOrManager() {
        boolean isAdminOrManager = userContextService.isAdminOrManager();
        
        Map<String, Object> response = new HashMap<>();
        response.put("isAdminOrManager", isAdminOrManager);
        response.put("hasAdminRole", SecurityContextUtils.hasRole("ADMIN"));
        response.put("hasManagerRole", SecurityContextUtils.hasRole("MANAGER"));
        response.put("userId", SecurityContextUtils.getCurrentUserId().orElse("unknown"));
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get all current user context information
     */
    @GetMapping("/full-context")
    public ResponseEntity<?> getFullContext() {
        if (!SecurityContextUtils.isAuthenticated()) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("No authenticated user found"));
        }

        UserPrincipal user = SecurityContextUtils.requireCurrentUserPrincipal();
        
        Map<String, Object> context = new HashMap<>();
        context.put("user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "branchId", user.getBranchId(),
                "active", user.isActive()
        ));
        context.put("authorities", user.getAuthorities());
        context.put("permissions", Map.of(
                "canManageUsers", userContextService.canManageUsers(),
                "isAdminOrManager", userContextService.isAdminOrManager(),
                "hasUserCreate", SecurityContextUtils.hasAuthority("USER_CREATE"),
                "hasSystemMonitor", SecurityContextUtils.hasAuthority("SYSTEM_MONITOR")
        ));
        context.put("roles", Map.of(
                "isAdmin", SecurityContextUtils.hasRole("ADMIN"),
                "isManager", SecurityContextUtils.hasRole("MANAGER"),
                "isUserManager", SecurityContextUtils.hasRole("USER_MANAGER"),
                "isLoanOfficer", SecurityContextUtils.hasRole("LOAN_OFFICER")
        ));
        context.put("auditInfo", userContextService.getCurrentUserForAudit());
        context.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(context);
    }
} 