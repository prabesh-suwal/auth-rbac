package com.sb.authenticationrbac.permissionsync.audit;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@Document(collection = "permission_sync_audit")
public class PermissionSyncAudit {
    @Id
    private String id;
    
    private LocalDateTime timestamp;
    private String status; // SUCCESS, VALIDATION_ERROR, SYSTEM_ERROR
    private String errorMessage;
    
    private int permissionsAdded;
    private int permissionsUpdated;
    private int permissionsTotal;
    
    private long executionTimeMs;
    private Map<String, Object> metadata;
    private List<String> validationErrors;
    
    private String triggeredBy; // API, SCHEDULER
    private String triggeredByUser;

    // Detailed change tracking
    private List<PermissionChange> changes;
    
    @Data
    @Builder
    public static class PermissionChange {
        private String permissionName;
        private String changeType; // ADDED, MODIFIED
        private Map<String, Object> oldValues; // Only for MODIFIED
        private Map<String, Object> newValues;
        private List<String> modifiedFields; // List of fields that were changed
    }
} 