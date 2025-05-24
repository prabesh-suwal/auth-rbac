package com.sb.authenticationrbac.permissionsync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sb.authenticationrbac.entities.*;
import com.sb.authenticationrbac.permissionsync.audit.PermissionSyncAudit;
import com.sb.authenticationrbac.permissionsync.audit.PermissionSyncAuditService;
import com.sb.authenticationrbac.permissionsync.dto.*;
import com.sb.authenticationrbac.permissionsync.notification.PermissionSyncNotifier;
import com.sb.authenticationrbac.permissionsync.notification.SyncResult;
import com.sb.authenticationrbac.permissionsync.validation.PermissionDefinitionValidator;
import com.sb.authenticationrbac.permissionsync.validation.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class PermissionSyncService {

    private final MongoTemplate mongoTemplate;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final PermissionDefinitionValidator validator;
    private final PermissionSyncNotifier notifier;
    private final PermissionSyncAuditService auditService;

    public PermissionSyncService(MongoTemplate mongoTemplate,
                               ResourceLoader resourceLoader,
                               ObjectMapper objectMapper,
                               PermissionDefinitionValidator validator,
                               PermissionSyncNotifier notifier,
                               PermissionSyncAuditService auditService) {
        this.mongoTemplate = mongoTemplate;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.notifier = notifier;
        this.auditService = auditService;
    }

    @Scheduled(cron = "${permission.sync.cron:0 0 * * * *}") // Default: every hour
    @Transactional
    public void syncPermissions() {
        syncPermissions("SCHEDULER", "system");
    }

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        syncPermissions();
    }

    @Transactional
    public void syncPermissions(String triggeredBy, String triggeredByUser) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("Starting permission synchronization");
            
            // Load permission definitions from JSON
            PermissionDefinitionsRoot definitions = loadPermissionDefinitions();
            
            // Convert and save permissions
            List<Permission> permissions = new ArrayList<>();
            try {
                definitions.getApiGroups().forEach((groupName, group) -> {
                    group.getPermissions().forEach(permDef -> {
                        try {
                            Permission permission = convertToPermission(permDef);
                            permissions.add(permission);
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to convert permission '" + permDef.getName() + "': " + e.getMessage(), e);
                        }
                    });
                });
            } catch (Exception e) {
                log.error("Error during permission conversion: {}", e.getMessage());
                notifier.notifySyncError(e);
                auditService.logSystemError(e, triggeredBy, triggeredByUser);
                throw e;
            }
            
            // Sync with database and track changes
            SyncResult result;
            try {
                result = syncWithDatabase(permissions);
                result.setTimeTaken(System.currentTimeMillis() - startTime);
            } catch (Exception e) {
                log.error("Error during database sync: {}", e.getMessage());
                notifier.notifySyncError(e);
                auditService.logSystemError(e, triggeredBy, triggeredByUser);
                throw e;
            }
            
            // Notify and audit success
            notifier.notifySuccess(result);
            auditService.logSuccessfulSync(result, triggeredBy, triggeredByUser);
            
            log.info("Permission synchronization completed successfully");
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null || !errorMsg.contains("permission '")) {
                errorMsg = "Permission sync failed: " + errorMsg;
            }
            log.error(errorMsg, e);
            notifier.notifySyncError(e);
            auditService.logSystemError(e, triggeredBy, triggeredByUser);
            throw new RuntimeException(errorMsg, e);
        }
    }

    private List<ValidationResult> validatePermissions(PermissionDefinitionsRoot definitions) {
        List<ValidationResult> results = new ArrayList<>();
        
        definitions.getApiGroups().forEach((groupName, group) -> {
            group.getPermissions().forEach(permDef -> {
                ValidationResult result = validator.validate(permDef);
                results.add(result);
            });
        });
        
        return results;
    }

    private PermissionDefinitionsRoot loadPermissionDefinitions() throws IOException {
        Resource resource = resourceLoader.getResource("classpath:permission-definitions.json");
        return objectMapper.readValue(resource.getInputStream(), PermissionDefinitionsRoot.class);
    }

    private Permission convertToPermission(PermissionDefinition def) {
        Permission permission = new Permission();
        permission.setName(def.getName());
        permission.setResource(def.getResource());
        permission.setAction(def.getOperation());
        permission.setDescription(def.getDescription());
        
        // Set the config directly from the definition
        permission.setConfig(def.getConfig());
        
        permission.setCreatedAt(LocalDateTime.now());
        permission.setUpdatedAt(LocalDateTime.now());
        permission.setActive(true);
        
        return permission;
    }

    @Transactional
    protected SyncResult syncWithDatabase(List<Permission> permissions) {
        int added = 0;
        int updated = 0;
        List<PermissionSyncAudit.PermissionChange> changes = new ArrayList<>();
        
        for (Permission permission : permissions) {
            try {
                Permission existing = mongoTemplate.findOne(
                    org.springframework.data.mongodb.core.query.Query.query(
                        org.springframework.data.mongodb.core.query.Criteria.where("name").is(permission.getName())
                    ),
                    Permission.class
                );
                
                if (existing != null) {
                    // Track changes for update
                    Map<String, Object> oldValues = new HashMap<>();
                    Map<String, Object> newValues = new HashMap<>();
                    List<String> modifiedFields = new ArrayList<>();
                    
                    // Compare and track changes
                    if (!Objects.equals(existing.getResource(), permission.getResource())) {
                        oldValues.put("resource", existing.getResource());
                        newValues.put("resource", permission.getResource());
                        modifiedFields.add("resource");
                    }
                    if (!Objects.equals(existing.getAction(), permission.getAction())) {
                        oldValues.put("action", existing.getAction());
                        newValues.put("action", permission.getAction());
                        modifiedFields.add("action");
                    }
                    if (!Objects.equals(existing.getDescription(), permission.getDescription())) {
                        oldValues.put("description", existing.getDescription());
                        newValues.put("description", permission.getDescription());
                        modifiedFields.add("description");
                    }
                    if (!Objects.equals(existing.getConfig(), permission.getConfig())) {
                        oldValues.put("config", existing.getConfig());
                        newValues.put("config", permission.getConfig());
                        modifiedFields.add("config");
                    }
                    
                    // If any changes were detected
                    if (!modifiedFields.isEmpty()) {
                        changes.add(PermissionSyncAudit.PermissionChange.builder()
                            .permissionName(permission.getName())
                            .changeType("MODIFIED")
                            .oldValues(oldValues)
                            .newValues(newValues)
                            .modifiedFields(modifiedFields)
                            .build());
                    }
                    
                    // Update existing permission
                    permission.setId(existing.getId());
                    permission.setCreatedAt(existing.getCreatedAt());
                    permission.setUpdatedAt(LocalDateTime.now());
                    mongoTemplate.save(permission);
                    updated++;
                    log.info("Updated permission: {}", permission.getName());
                } else {
                    // Track new permission
                    Map<String, Object> newValues;
                    try {
                        newValues = objectMapper.convertValue(permission, Map.class);
                    } catch (IllegalArgumentException e) {
                        throw new RuntimeException("Failed to process permission '" + permission.getName() + "': " + e.getMessage(), e);
                    }
                    
                    changes.add(PermissionSyncAudit.PermissionChange.builder()
                        .permissionName(permission.getName())
                        .changeType("ADDED")
                        .newValues(newValues)
                        .modifiedFields(List.of("all"))
                        .build());
                    
                    // Create new permission
                    mongoTemplate.save(permission);
                    added++;
                    log.info("Created new permission: {}", permission.getName());
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to process permission '" + permission.getName() + "': " + e.getMessage(), e);
            }
        }
        
        // Create audit entry with changes
        PermissionSyncAudit audit = PermissionSyncAudit.builder()
            .timestamp(LocalDateTime.now())
            .status("SUCCESS")
            .permissionsAdded(added)
            .permissionsUpdated(updated)
            .permissionsTotal(permissions.size())
            .changes(changes)
            .build();
        
        mongoTemplate.save(audit);
        
        return SyncResult.builder()
            .added(added)
            .updated(updated)
            .total(permissions.size())
            .build();
    }
} 