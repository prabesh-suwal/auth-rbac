package com.sb.authenticationrbac.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Document(collection = "system_config")
public class SystemConfig {
    @Id
    private String id;
    private String configType; // "PERMISSION_DEFAULTS", "BRANCH_HIERARCHY", etc.
    private Map<String, Object> configuration;
    private LocalDateTime updatedAt;
    
    // constructors, getters, setters
}