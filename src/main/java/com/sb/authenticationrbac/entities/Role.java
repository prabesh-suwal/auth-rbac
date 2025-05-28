package com.sb.authenticationrbac.entities;

import com.sb.authenticationrbac.role.dto.RoleConfiguration;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.*;

@Data
@Document(collection = "roles")
public class Role {
    @Id
    private String id;
    private String name;
    private String description;
    private String parentRoleName; // Optional: for role hierarchy

    @DBRef
    private List<Permission> permissions; // Permission IDs
    private RoleConfiguration configuration; // Dynamic role configuration
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean active = true;
    
    // constructors, getters, setters
}