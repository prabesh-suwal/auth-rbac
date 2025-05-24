package com.sb.authenticationrbac.permissionsync.controller;

import com.sb.authenticationrbac.permissionsync.service.PermissionSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/permissions")
public class PermissionSyncController {

    private final PermissionSyncService permissionSyncService;

    public PermissionSyncController(PermissionSyncService permissionSyncService) {
        this.permissionSyncService = permissionSyncService;
    }

    @PostMapping("/sync")
    public ResponseEntity<String> syncPermissions() {
        try {
            permissionSyncService.syncPermissions();
            return ResponseEntity.ok("Permission synchronization completed successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Permission synchronization failed: " + e.getMessage());
        }
    }
} 