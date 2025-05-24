package com.sb.authenticationrbac.controllers;
import com.sb.authenticationrbac.aop.CheckPermission;
import com.sb.authenticationrbac.dtos.requests.CreateBranchRequest;
import com.sb.authenticationrbac.entities.Branch;
import com.sb.authenticationrbac.entities.User;
import com.sb.authenticationrbac.services.DynamicPermissionManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/branches")
public class BranchController {

    private final DynamicPermissionManagementService permissionService;

    public BranchController(DynamicPermissionManagementService permissionService) {
        this.permissionService = permissionService;
    }

    @PostMapping
    @CheckPermission(
        value = "LOAN_APPROVE",
        operation = "CREATE",
        contextParams = {"parentBranchId", "branchLevel"}
    )
    public ResponseEntity<Branch> createBranch(@RequestBody CreateBranchRequest request) {
        Branch branch = permissionService.createBranch(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(branch);
    }
//
//    @GetMapping("/{branchId}/users")
//    @CheckPermission(
//        value = "BRANCH_USERS_VIEW",
//        resource = "BRANCH",
//        resourceIdParam = "branchId",
//        operation = "VIEW_USERS"
//    )
//    public ResponseEntity<List<User>> getBranchUsers(@PathVariable String branchId) {
//        List<User> users = branchService.getBranchUsers(branchId);
//        return ResponseEntity.ok(users);
//    }
//
//    @PutMapping("/{branchId}/config")
//    @CheckPermission(
//        value = "BRANCH_CONFIGURE",
//        resource = "BRANCH",
//        resourceIdParam = "branchId",
//        operation = "UPDATE_CONFIG"
//    )
//    public ResponseEntity<Branch> updateBranchConfig(@PathVariable String branchId,
//                                                   @RequestBody Map<String, Object> config) {
//        Branch branch = branchService.updateBranchConfig(branchId, config);
//        return ResponseEntity.ok(branch);
//    }
}