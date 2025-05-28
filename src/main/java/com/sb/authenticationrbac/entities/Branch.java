package com.sb.authenticationrbac.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Document(collection = "branches")
public class Branch {
    @Id
    private String id;
    private String name;
    private String code;
    private String location;
    private String address;
    private String parentBranchId; // For hierarchy
    private List<String> childBranchIds;
    private Map<String, Object> metadata;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // constructors, getters, setters
}
