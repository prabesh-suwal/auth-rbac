package com.sb.authenticationrbac.dtos.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class CreateUserRequest {
    @NotBlank(message = "Username is required")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username can only contain letters, numbers, dots, underscores and hyphens")
    private String username;


    private String password;
    @NotBlank(message = "password is required")


    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Branch ID is required")
    private String branchId;

    @NotEmpty(message = "At least one role is required")
    private List<String> roleIds;

    @Valid
    private UserPermissionConfig permissionConfig;

    private boolean active = true;
} 