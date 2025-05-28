package com.sb.authenticationrbac.entities;

// Remove the DTO import and use the entity from the same package
// import com.sb.authenticationrbac.dtos.requests.UserPermissionConfig;
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
    private UserPermissionConfig permissionConfig;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
