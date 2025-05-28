package com.sb.authenticationrbac.role.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class RoleDefinition {
    @NotBlank(message = "Role name is required")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "Role name must be uppercase with underscores and numbers only")
    private String name;

    @NotBlank(message = "Role description is required")
    @Size(min = 10, max = 200, message = "Description must be between 10 and 200 characters")
    private String description;

    @NotEmpty(message = "At least one permission is required")
    @JsonProperty(value = "permissionIds")
    private List<@NotBlank(message = "Permission cannot be blank") String> permissionIds;

    @Valid
    @NotNull(message = "Role configuration is required")
    private RoleConfiguration configuration;

    private boolean active = true;
} 