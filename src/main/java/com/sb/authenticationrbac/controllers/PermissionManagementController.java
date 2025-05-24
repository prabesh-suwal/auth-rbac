package com.sb.authenticationrbac.controllers;

import com.sb.authenticationrbac.aop.CheckPermission;
import com.sb.authenticationrbac.dtos.requests.*;
import com.sb.authenticationrbac.entities.Permission;
import com.sb.authenticationrbac.entities.Role;
import com.sb.authenticationrbac.role.dto.RoleDefinition;
import com.sb.authenticationrbac.services.DynamicPermissionManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/permissions")
public class PermissionManagementController {

    private final DynamicPermissionManagementService permissionService;

    public PermissionManagementController(DynamicPermissionManagementService permissionService) {
        this.permissionService = permissionService;
    }

    // Super admin operations - configuration stored in database
    @PostMapping("/permissions")
//    @CheckPermission(
//        value = "PERMISSION_MANAGE",
//        operation = "CREATE",
//        message = "Only authorized users can create permissions"
//    )
    public ResponseEntity<Permission> createPermission(@RequestBody CreatePermissionRequest request) {
        Permission permission = permissionService.createPermission(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(permission);
    }



    // Update role configuration dynamically
    @PutMapping("/roles/{roleId}/config")
    @CheckPermission(
        value = "ROLE_CONFIGURE",
        operation = "UPDATE",
        resourceIdParam = "roleId"
    )
    public ResponseEntity<Role> updateRoleConfig(@PathVariable String roleId, 
                                               @RequestBody Map<String, Object> configuration) {
        Role role = permissionService.updateRoleConfiguration(roleId, configuration);
        return ResponseEntity.ok(role);
    }

    // Assign role to user with dynamic validation
    @PostMapping("/users/{userId}/roles/{roleId}")
    @CheckPermission(
        value = "USER_ROLE_ASSIGN",
        resource = "USER",
        resourceIdParam = "userId",
        operation = "ASSIGN_ROLE",
        contextParams = {"roleId", "assignmentReason"}
    )
    public ResponseEntity<Void> assignRole(@PathVariable String userId,
                                         @PathVariable String roleId,
                                         @RequestParam(required = false) String assignmentReason) {
        permissionService.assignRoleToUser(userId, roleId);
        return ResponseEntity.ok().build();
    }

    // Set user-specific amount limits
    @PostMapping("/users/{userId}/amount-limits")
    @CheckPermission(
        value = "USER_LIMIT_MANAGE",
        resource = "USER",
        resourceIdParam = "userId",
        operation = "SET_LIMIT"
    )
    public ResponseEntity<Void> setAmountLimit(@PathVariable String userId,
                                             @RequestBody AmountLimitRequest request) {
        permissionService.setUserAmountLimit(userId, request.getPermission(), request.getLimit());
        return ResponseEntity.ok().build();
    }

//    // Grant temporary permissions with conditions
//    @PostMapping("/users/{userId}/temporary-permissions")
//    @CheckPermission(
//        value = "TEMP_PERMISSION_GRANT",
//        resource = "USER",
//        resourceIdParam = "userId",
//        operation = "GRANT_TEMP",
//        contextParams = {"duration", "permissionType"}
//    )
//    public ResponseEntity<Void> grantTemporaryPermission(@PathVariable String userId,
//                                                       @RequestBody TemporaryPermissionRequest request,
//                                                       Authentication authentication) {
//        CustomUserDetails grantor = (CustomUserDetails) authentication.getPrincipal();
//
//        permissionService.grantTemporaryPermission(
//            userId,
//            request.getPermissionId(),
//            request.getExpiresAt(),
//            grantor.getUserId(),
//            request.getReason(),
//            request.getConditions()
//        );
//        return ResponseEntity.ok().build();
//    }

    // Resource-specific access management
    @PostMapping("/users/{userId}/resource-access")
    @CheckPermission(
        value = "RESOURCE_ACCESS_MANAGE",
        resource = "USER",
        resourceIdParam = "userId",
        operation = "GRANT_RESOURCE"
    )
    public ResponseEntity<Void> grantResourceAccess(@PathVariable String userId,
                                                  @RequestBody ResourceAccessRequest request) {
        permissionService.grantResourceAccess(
            userId,
            request.getResourceType(),
            request.getResourceId(),
            request.getOperations(),
            request.getExpiresAt()
        );
        return ResponseEntity.ok().build();
    }

    // Get user permission summary
    @GetMapping("/users/{userId}/summary")
    @CheckPermission(
        value = "PERMISSION_VIEW",
        resource = "USER", 
        resourceIdParam = "userId",
        operation = "VIEW_PERMISSIONS"
    )
    public ResponseEntity<Map<String, Object>> getUserPermissionSummary(@PathVariable String userId) {
        Map<String, Object> summary = permissionService.getUserPermissionSummary(userId);
        return ResponseEntity.ok(summary);
    }
}