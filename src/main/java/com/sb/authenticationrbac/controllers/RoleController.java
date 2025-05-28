package com.sb.authenticationrbac.controllers;

import com.sb.authenticationrbac.aop.CheckPermission;
import com.sb.authenticationrbac.entities.Role;
import com.sb.authenticationrbac.role.dto.RoleDefinition;
import com.sb.authenticationrbac.services.RoleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    @CheckPermission(
            value = "ROLE_CREATE",
            operation = "CREATE"
    )
    public ResponseEntity<Role> createRole(@RequestBody RoleDefinition request) {
        Role role = this.roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(role);
    }

}
