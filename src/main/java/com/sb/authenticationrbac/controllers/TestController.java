package com.sb.authenticationrbac.controllers;

import com.sb.authenticationrbac.aop.CheckPermission;
import com.sb.authenticationrbac.aop.CheckPermissions;
import com.sb.authenticationrbac.dtos.responses.MessageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/public")
    public ResponseEntity<MessageResponse> publicEndpoint() {
        return ResponseEntity.ok(new MessageResponse("This is a public endpoint - no authentication required"));
    }

    @GetMapping("/authenticated")
    public ResponseEntity<MessageResponse> authenticatedEndpoint() {
        return ResponseEntity.ok(new MessageResponse("This endpoint requires authentication"));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> adminEndpoint() {
        return ResponseEntity.ok(new MessageResponse("This endpoint requires ADMIN role"));
    }

    @GetMapping("/user-create")
    @CheckPermission(value = "USER_CREATE", operation = "CREATE")
    public ResponseEntity<MessageResponse> userCreateEndpoint() {
        return ResponseEntity.ok(new MessageResponse("This endpoint requires USER_CREATE permission"));
    }

    @GetMapping("/loan-view")
    @CheckPermission(value = "LOAN_VIEW", operation = "VIEW")
    public ResponseEntity<MessageResponse> loanViewEndpoint() {
        return ResponseEntity.ok(new MessageResponse("This endpoint requires LOAN_VIEW permission"));
    }

    @GetMapping("/loan-approve")
    @CheckPermission(value = "LOAN_APPROVE", operation = "APPROVE")
    public ResponseEntity<MessageResponse> loanApproveEndpoint() {
        return ResponseEntity.ok(new MessageResponse("This endpoint requires LOAN_APPROVE permission"));
    }

    @GetMapping("/multiple-permissions")
    @CheckPermissions(
        value = {
            @CheckPermission(value = "USER_VIEW", operation = "VIEW"),
            @CheckPermission(value = "SYSTEM_MONITOR", operation = "VIEW")
        },
        logic = "OR",
        message = "Requires either USER_VIEW or SYSTEM_MONITOR permission"
    )
    public ResponseEntity<MessageResponse> multiplePermissionsEndpoint() {
        return ResponseEntity.ok(new MessageResponse("This endpoint requires USER_VIEW OR SYSTEM_MONITOR permission"));
    }

    @GetMapping("/system-monitor")
    @CheckPermission(value = "SYSTEM_MONITOR", operation = "VIEW")
    public ResponseEntity<MessageResponse> systemMonitorEndpoint() {
        return ResponseEntity.ok(new MessageResponse("This endpoint requires SYSTEM_MONITOR permission"));
    }
} 