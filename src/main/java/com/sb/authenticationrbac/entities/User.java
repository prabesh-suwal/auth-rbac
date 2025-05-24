package com.sb.authenticationrbac.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String username;
    private String email;
    private String password;
    private String branchId;
    private List<String> roleIds;
    
    // User-specific permission overrides
    private UserPermissionConfig permissionConfig;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean active = true;
    
    // constructors, getters, setters
}
