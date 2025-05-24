package com.sb.authenticationrbac.rolesync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sb.authenticationrbac.entities.Permission;
import com.sb.authenticationrbac.entities.Role;
import com.sb.authenticationrbac.entities.RoleConfiguration;
import com.sb.authenticationrbac.entities.RoleApprovalChainConfig;
import com.sb.authenticationrbac.entities.RoleAuditFeaturesConfig;
import com.sb.authenticationrbac.entities.RoleBranchRestrictionsConfig;
import com.sb.authenticationrbac.entities.RoleRiskAssessmentFeaturesConfig;
import com.sb.authenticationrbac.entities.RoleWorkingHoursConfig;
import com.sb.authenticationrbac.repositories.PermissionRepository;
import com.sb.authenticationrbac.repositories.RoleRepository;
import com.sb.authenticationrbac.rolesync.dto.RoleDefinitionInput;
import com.sb.authenticationrbac.rolesync.dto.RoleDefinitionsRootInput;
import com.sb.authenticationrbac.rolesync.dto.RoleConfigurationInput;
import com.sb.authenticationrbac.rolesync.dto.ApprovalChainConfigInput;
import com.sb.authenticationrbac.rolesync.dto.AuditFeaturesConfigInput;
import com.sb.authenticationrbac.rolesync.dto.BranchRestrictionsConfigInput;
import com.sb.authenticationrbac.rolesync.dto.RiskAssessmentFeaturesConfigInput;
import com.sb.authenticationrbac.rolesync.dto.WorkingHoursConfigInput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RoleSyncService {

    private final MongoTemplate mongoTemplate;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Value("classpath:role-definitions.json")
    private Resource roleDefinitionsResource;

    public RoleSyncService(MongoTemplate mongoTemplate,
                           RoleRepository roleRepository,
                           PermissionRepository permissionRepository,
                           ObjectMapper objectMapper,
                           ResourceLoader resourceLoader) {
        this.mongoTemplate = mongoTemplate;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
    }

    public Optional<RoleDefinitionsRootInput> loadRoleDefinitions() {
        try (InputStream inputStream = roleDefinitionsResource.getInputStream()) {
            RoleDefinitionsRootInput root = objectMapper.readValue(inputStream, RoleDefinitionsRootInput.class);
            log.info("Successfully loaded {} role definitions from JSON.", root.getRoles().size());
            return Optional.of(root);
        } catch (Exception e) {
            log.error("Failed to load or parse role-definitions.json: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    private Role convertToRoleEntity(RoleDefinitionInput dto, List<String> finalPermissions) {
        Role role = new Role();
        role.setName(dto.getName());
        role.setDescription(dto.getDescription());
        role.setActive(dto.isActive());
        role.setParentRoleName(dto.getParentRoleName()); // Set parent role name

        // Set the fully resolved (flattened) permissions
        role.setPermissions(finalPermissions != null ? finalPermissions : new ArrayList<>());

        // Map configuration
        if (dto.getConfiguration() != null) {
            role.setConfiguration(mapRoleConfiguration(dto.getConfiguration()));
        }

        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        return role;
    }

    private RoleConfiguration mapRoleConfiguration(RoleConfigurationInput configDto) {
        if (configDto == null) {
            return null;
        }
        RoleConfiguration config = new RoleConfiguration();
        config.setDefaultAmountLimits(configDto.getDefaultAmountLimits());

        if (configDto.getWorkingHours() != null) {
            WorkingHoursConfigInput whDto = configDto.getWorkingHours();
            config.setWorkingHours(new RoleWorkingHoursConfig(whDto.isEnabled(), whDto.getStartTime(), whDto.getEndTime(), whDto.getWorkingDays()));
        }

        if (configDto.getBranchRestrictions() != null) {
            BranchRestrictionsConfigInput brDto = configDto.getBranchRestrictions();
            config.setBranchRestrictions(new RoleBranchRestrictionsConfig(brDto.getType(), brDto.isAllowCrossBranchView()));
        }

        if (configDto.getApprovalChain() != null) {
            Map<String, RoleApprovalChainConfig> approvalChainMap = configDto.getApprovalChain().entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> {
                        ApprovalChainConfigInput acDto = entry.getValue();
                        return new RoleApprovalChainConfig(acDto.isEnabled(), acDto.getAmountThreshold());
                    }
                ));
            config.setApprovalChain(approvalChainMap);
        }
        
        if (configDto.getAuditFeatures() != null) {
            AuditFeaturesConfigInput afDto = configDto.getAuditFeatures();
            config.setAuditFeatures(new RoleAuditFeaturesConfig(afDto.isCanViewDeletedRecords(), afDto.isCanViewAuditHistory()));
        }

        if (configDto.getRiskAssessmentFeatures() != null) {
            RiskAssessmentFeaturesConfigInput rafDto = configDto.getRiskAssessmentFeatures();
            config.setRiskAssessmentFeatures(new RoleRiskAssessmentFeaturesConfig(rafDto.isCanOverrideRiskScores(), rafDto.isCanModifyRiskParameters()));
        }
        return config;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "${role.sync.cron:0 1 * * * *}") // Default: every hour at minute 1
    public void syncRoles() {
        syncRoles("SYSTEM_STARTUP", "SYSTEM");
    }

    public void syncRoles(String triggeredBy, String triggeredByUser) {
        log.info("Role synchronization process started by {} (User: {}).", triggeredBy, triggeredByUser);
        Optional<RoleDefinitionsRootInput> optRoot = loadRoleDefinitions();

        if (optRoot.isEmpty() || CollectionUtils.isEmpty(optRoot.get().getRoles())) {
            log.warn("No role definitions found or failed to load. Skipping synchronization.");
            return;
        }

        RoleDefinitionsRootInput root = optRoot.get();
        Map<String, RoleDefinitionInput> roleDefinitionsMap = root.getRoles().stream()
            .collect(Collectors.toMap(RoleDefinitionInput::getName, dto -> dto));
        
        Map<String, Set<String>> resolvedPermissionsCache = new HashMap<>();
        int newRolesCount = 0;
        int updatedRolesCount = 0;

        // First pass: resolve all permissions
        for (RoleDefinitionInput dto : root.getRoles()) {
            try {
                resolvePermissions(dto.getName(), roleDefinitionsMap, resolvedPermissionsCache, new ArrayList<>());
            } catch (Exception e) {
                log.error("Error resolving permissions for role definition '{}': {}", dto.getName(), e.getMessage(), e);
                // Optionally, decide if this role should be skipped or if sync should halt
            }
        }

        // Second pass: create/update roles with resolved permissions
        for (RoleDefinitionInput dto : root.getRoles()) {
            try {
                Optional<Role> existingRoleOpt = roleRepository.findByName(dto.getName());
                Role roleToSave;
                
                List<String> finalPermissions = new ArrayList<>(resolvedPermissionsCache.getOrDefault(dto.getName(), new HashSet<>()));

                if (existingRoleOpt.isPresent()) {
                    roleToSave = existingRoleOpt.get();
                    // Update existing role
                    roleToSave.setDescription(dto.getDescription());
                    roleToSave.setActive(dto.isActive());
                    roleToSave.setParentRoleName(dto.getParentRoleName());
                    roleToSave.setPermissions(finalPermissions);
                    
                    if (dto.getConfiguration() != null) {
                        roleToSave.setConfiguration(mapRoleConfiguration(dto.getConfiguration()));
                    } else {
                        roleToSave.setConfiguration(null); // Or set to a default empty config
                    }
                    roleToSave.setUpdatedAt(LocalDateTime.now());
                    updatedRolesCount++;
                    log.debug("Updating existing role: {}", roleToSave.getName());
                } else {
                    // Pass the final resolved permissions to convertToRoleEntity
                    roleToSave = convertToRoleEntity(dto, finalPermissions);
                    // convertToRoleEntity already sets parentRoleName, createdAt, updatedAt
                    newRolesCount++;
                    log.debug("Creating new role: {}", roleToSave.getName());
                }
                roleRepository.save(roleToSave);
            } catch (Exception e) {
                log.error("Error processing role definition '{}' for persistence: {}", dto.getName(), e.getMessage(), e);
            }
        }
        log.info("Role synchronization finished. New roles: {}, Updated roles: {}.", newRolesCount, updatedRolesCount);
        // Optional: Implement SyncResult and audit logging similar to PermissionSyncService
    }

    private Set<String> resolvePermissions(String roleName,
                                           Map<String, RoleDefinitionInput> allDtosMap,
                                           Map<String, Set<String>> resolvedCache,
                                           List<String> processingStack) {
        if (resolvedCache.containsKey(roleName)) {
            return resolvedCache.get(roleName);
        }

        if (processingStack.contains(roleName)) {
            log.error("Circular dependency detected in role hierarchy involving role: {}. Path: {}", roleName, String.join(" -> ", processingStack) + " -> " + roleName);
            throw new IllegalStateException("Circular dependency for role: " + roleName);
        }
        processingStack.add(roleName);

        RoleDefinitionInput currentDto = allDtosMap.get(roleName);
        if (currentDto == null) {
            log.warn("Role definition not found for name: {}. Skipping its permission resolution.", roleName);
            processingStack.remove(processingStack.size() - 1); // Backtrack
            return new HashSet<>(); // Return empty set if role DTO is not found
        }

        Set<String> currentPermissions = new HashSet<>();

        // 1. Add direct permissions
        if (!CollectionUtils.isEmpty(currentDto.getPermissions())) {
            currentDto.getPermissions().stream()
                .map(permissionName -> {
                    Query query = Query.query(Criteria.where("name").is(permissionName));
                    Permission p = mongoTemplate.findOne(query, Permission.class);
                    if (p == null) {
                        log.warn("Permission with name '{}' not found for role '{}'. Skipping direct permission.", permissionName, currentDto.getName());
                        return null;
                    }
                    return p.getId();
                })
                .filter(id -> id != null)
                .forEach(currentPermissions::add);
        }

        // 2. Add parent's permissions
        if (currentDto.getParentRoleName() != null && !currentDto.getParentRoleName().isEmpty()) {
            if (!allDtosMap.containsKey(currentDto.getParentRoleName())) {
                log.warn("Parent role '{}' for role '{}' not found in definitions. Skipping parent permissions.", currentDto.getParentRoleName(), roleName);
            } else {
                 // Recursively resolve parent permissions
                Set<String> parentPermissions = resolvePermissions(currentDto.getParentRoleName(), allDtosMap, resolvedCache, processingStack);
                currentPermissions.addAll(parentPermissions);
            }
        }
        
        resolvedCache.put(roleName, currentPermissions);
        processingStack.remove(processingStack.size() - 1); // Backtrack
        return currentPermissions;
    }
}
