package com.sb.authenticationrbac.services;// Dynamic Permission Management Service

import com.sb.authenticationrbac.dtos.requests.CreateBranchRequest;
import com.sb.authenticationrbac.dtos.requests.CreatePermissionRequest;
import com.sb.authenticationrbac.dtos.requests.CreateRoleRequest;
import com.sb.authenticationrbac.entities.*;
import com.sb.authenticationrbac.exceptions.UserNotFoundException;
import com.sb.authenticationrbac.role.dto.RoleDefinition;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class DynamicPermissionManagementService {
    
    private final MongoTemplate mongoTemplate;
    
    public DynamicPermissionManagementService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }
    
    // ===== PERMISSION MANAGEMENT =====
    
    public Permission createPermission(CreatePermissionRequest request) {
        Permission permission = new Permission();
        permission.setId(UUID.randomUUID().toString());
        permission.setName(request.getName());
        permission.setResource(request.getResource());
        permission.setAction(request.getAction());
        permission.setDescription(request.getDescription());
        permission.setConfig(request.getConfig());
        permission.setCreatedAt(LocalDateTime.now());
        permission.setUpdatedAt(LocalDateTime.now());
        permission.setActive(true);
        
        return mongoTemplate.save(permission);
    }
    
    public Permission updatePermissionConfig(String permissionId, PermissionConfig config) {
        Query query = Query.query(Criteria.where("id").is(permissionId));
        Update update = Update.update("config", config)
                            .set("updatedAt", LocalDateTime.now());
        
        mongoTemplate.updateFirst(query, update, Permission.class);
        return mongoTemplate.findById(permissionId, Permission.class);
    }

    public Role updateRoleConfiguration(String roleId, Map<String, Object> configuration) {
        Query query = Query.query(Criteria.where("id").is(roleId));
        Update update = Update.update("configuration", configuration)
                            .set("updatedAt", LocalDateTime.now());
        
        mongoTemplate.updateFirst(query, update, Role.class);
        return mongoTemplate.findById(roleId, Role.class);
    }
    
    public void assignPermissionToRole(String roleId, String permissionId) {
        Query query = Query.query(Criteria.where("id").is(roleId));
        Update update = new Update().addToSet("permissions", permissionId)
                                  .set("updatedAt", LocalDateTime.now());
        
        mongoTemplate.updateFirst(query, update, Role.class);
    }
    
    public void removePermissionFromRole(String roleId, String permissionId) {
        Query query = Query.query(Criteria.where("id").is(roleId));
        Update update = new Update().pull("permissions", permissionId)
                                  .set("updatedAt", LocalDateTime.now());
        
        mongoTemplate.updateFirst(query, update, Role.class);
    }
    
    // ===== USER PERMISSION MANAGEMENT =====
    
    public void assignRoleToUser(String userId, String roleId) {
        Query query = Query.query(Criteria.where("id").is(userId));
        Update update = new Update().addToSet("roleIds", roleId)
                                  .set("updatedAt", LocalDateTime.now());
        
        mongoTemplate.updateFirst(query, update, User.class);
    }
    
    public void removeRoleFromUser(String userId, String roleId) {
        Query query = Query.query(Criteria.where("id").is(userId));
        Update update = new Update().pull("roleIds", roleId)
                                  .set("updatedAt", LocalDateTime.now());
        
        mongoTemplate.updateFirst(query, update, User.class);
    }
    
    public void setUserAmountLimit(String userId, String permission, Double limit) {
        User user = mongoTemplate.findById(userId, User.class);
        if (user == null) {
            throw new UserNotFoundException("User not found");
        }
        
        if (user.getPermissionConfig() == null) {
            user.setPermissionConfig(new UserPermissionConfig());
        }
        
        if (user.getPermissionConfig().getAmountLimitOverrides() == null) {
            user.getPermissionConfig().setAmountLimitOverrides(new HashMap<>());
        }
        
        user.getPermissionConfig().getAmountLimitOverrides().put(permission, limit);
        user.setUpdatedAt(LocalDateTime.now());
        
        mongoTemplate.save(user);
    }
    
    public void setUserBranchAccess(String userId, BranchAccessConfig branchAccess) {
        Query query = Query.query(Criteria.where("id").is(userId));
        Update update = Update.update("permissionConfig.branchAccessOverride", branchAccess)
                            .set("updatedAt", LocalDateTime.now());
        
        mongoTemplate.updateFirst(query, update, User.class);
    }
    
    public void grantResourceAccess(String userId, String resourceType, String resourceId, 
                                  List<String> operations, LocalDateTime expiresAt) {
        User user = mongoTemplate.findById(userId, User.class);
        if (user == null) {
            throw new UserNotFoundException("User not found");
        }
        
        if (user.getPermissionConfig() == null) {
            user.setPermissionConfig(new UserPermissionConfig());
        }
        
        if (user.getPermissionConfig().getResourceAccesses() == null) {
            user.getPermissionConfig().setResourceAccesses(new ArrayList<>());
        }
        
        ResourceAccess access = new ResourceAccess();
        access.setResourceType(resourceType);
        access.setResourceId(resourceId);
        access.setAccessType("GRANT");
        access.setAllowedOperations(operations);
        access.setExpiresAt(expiresAt);
        
        user.getPermissionConfig().getResourceAccesses().add(access);
        user.setUpdatedAt(LocalDateTime.now());
        
        mongoTemplate.save(user);
    }
    
    public void denyResourceAccess(String userId, String resourceType, String resourceId) {
        User user = mongoTemplate.findById(userId, User.class);
        if (user == null) {
            throw new UserNotFoundException("User not found");
        }
        
        if (user.getPermissionConfig() == null) {
            user.setPermissionConfig(new UserPermissionConfig());
        }
        
        if (user.getPermissionConfig().getResourceAccesses() == null) {
            user.getPermissionConfig().setResourceAccesses(new ArrayList<>());
        }
        
        ResourceAccess access = new ResourceAccess();
        access.setResourceType(resourceType);
        access.setResourceId(resourceId);
        access.setAccessType("DENY");
        
        user.getPermissionConfig().getResourceAccesses().add(access);
        user.setUpdatedAt(LocalDateTime.now());
        
        mongoTemplate.save(user);
    }
    
    public void grantTemporaryPermission(String userId, String permissionId, LocalDateTime expiresAt, 
                                       String grantedBy, String reason, Map<String, Object> conditions) {
        User user = mongoTemplate.findById(userId, User.class);
        if (user == null) {
            throw new UserNotFoundException("User not found");
        }
        
        if (user.getPermissionConfig() == null) {
            user.setPermissionConfig(new UserPermissionConfig());
        }
        
        if (user.getPermissionConfig().getTemporaryPermissions() == null) {
            user.getPermissionConfig().setTemporaryPermissions(new ArrayList<>());
        }
        
        TemporaryPermission tempPerm = new TemporaryPermission();
        tempPerm.setPermissionId(permissionId);
        tempPerm.setGrantedAt(LocalDateTime.now());
        tempPerm.setExpiresAt(expiresAt);
        tempPerm.setGrantedBy(grantedBy);
        tempPerm.setReason(reason);
        tempPerm.setConditions(conditions);
        
        user.getPermissionConfig().getTemporaryPermissions().add(tempPerm);
        user.setUpdatedAt(LocalDateTime.now());
        
        mongoTemplate.save(user);
    }
    
    // ===== BRANCH MANAGEMENT =====
    
    public Branch createBranch(CreateBranchRequest request) {
        Branch branch = new Branch();
        branch.setId(UUID.randomUUID().toString());
        branch.setName(request.getName());
        branch.setCode(request.getCode());
        branch.setLocation(request.getLocation());
        branch.setParentBranchId(request.getParentBranchId());
        branch.setMetadata(request.getMetadata());
        
        // Update parent branch if specified
        if (request.getParentBranchId() != null) {
            Query query = Query.query(Criteria.where("id").is(request.getParentBranchId()));
            Update update = new Update().addToSet("childBranchIds", branch.getId());
            mongoTemplate.updateFirst(query, update, Branch.class);
        }
        
        return mongoTemplate.save(branch);
    }
    
    // ===== CONFIGURATION TEMPLATES =====
    
    public PermissionConfig createStandardLoanApprovalConfig(Double defaultLimit, String branchAccessType) {
        PermissionConfig config = new PermissionConfig();
        
        // Branch access configuration
        BranchAccessConfig branchAccess = new BranchAccessConfig();
        branchAccess.setType(branchAccessType);
        branchAccess.setBranchFieldPath("branch.id");
        config.setBranchAccess(branchAccess);
        
        // Amount limit configuration
        AmountLimitConfig amountLimit = new AmountLimitConfig();
        amountLimit.setEnabled(true);
        amountLimit.setLimitType("ROLE_BASED");
        amountLimit.setDefaultLimit(defaultLimit);
        amountLimit.setAmountFieldPath("amount");
        config.setAmountLimit(amountLimit);
        
        // Time access configuration (business hours only)
        TimeAccessConfig timeAccess = new TimeAccessConfig();
        timeAccess.setEnabled(true);
        timeAccess.setAllowedDays(Arrays.asList("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"));
        
        TimeWindow businessHours = new TimeWindow();
        businessHours.setStartTime("09:00");
        businessHours.setEndTime("17:00");
        timeAccess.setAllowedTimeWindows(Collections.singletonList(businessHours));
        config.setTimeAccess(timeAccess);
        
        // Validation rules
        ValidationRule amountValidation = new ValidationRule();
        amountValidation.setName("positive_amount");
        amountValidation.setCondition("#resource.amount > 0");
        amountValidation.setErrorMessage("Loan amount must be positive");
        amountValidation.setPriority(1);
        
        ValidationRule statusValidation = new ValidationRule();
        statusValidation.setName("pending_status");
        statusValidation.setCondition("#resource.status == 'PENDING'");
        statusValidation.setErrorMessage("Can only approve pending loans");
        statusValidation.setPriority(2);
        
        config.setValidationRules(Arrays.asList(amountValidation, statusValidation));
        
        return config;
    }
    
    public PermissionConfig createReportViewConfig(String branchAccessType, boolean timeRestricted) {
        PermissionConfig config = new PermissionConfig();
        
        // Branch access
        BranchAccessConfig branchAccess = new BranchAccessConfig();
        branchAccess.setType(branchAccessType);
        config.setBranchAccess(branchAccess);
        
        // Time restrictions if needed
        if (timeRestricted) {
            TimeAccessConfig timeAccess = new TimeAccessConfig();
            timeAccess.setEnabled(true);
            timeAccess.setAllowedDays(Arrays.asList("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"));
            config.setTimeAccess(timeAccess);
        }
        
        return config;
    }
    
    // ===== PERMISSION QUERIES =====
    
    public List<Permission> getPermissionsByResource(String resource) {
        Query query = Query.query(Criteria.where("resource").is(resource).and("active").is(true));
        return mongoTemplate.find(query, Permission.class);
    }
    
    public List<Role> getRolesByUser(String userId) {
        User user = mongoTemplate.findById(userId, User.class);
        if (user == null || user.getRoleIds() == null) {
            return Collections.emptyList();
        }
        
        Query query = Query.query(Criteria.where("_id").in(user.getRoleIds()).and("active").is(true));
        return mongoTemplate.find(query, Role.class);
    }
    
    public List<Permission> getEffectivePermissions(String userId) {
        List<Role> userRoles = getRolesByUser(userId);
        Set<String> permissionIds = new HashSet<>();
        
        for (Role role : userRoles) {
            if (role.getPermissions() != null) {
                permissionIds.addAll(role.getPermissions());
            }
        }
        
        if (permissionIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        Query query = Query.query(Criteria.where("_id").in(permissionIds).and("active").is(true));
        return mongoTemplate.find(query, Permission.class);
    }
    
    public Map<String, Object> getUserPermissionSummary(String userId) {
        Map<String, Object> summary = new HashMap<>();
        
        User user = mongoTemplate.findById(userId, User.class);
        if (user == null) {
            return summary;
        }
        
        summary.put("userId", userId);
        summary.put("username", user.getUsername());
        summary.put("branchId", user.getBranchId());
        summary.put("roles", getRolesByUser(userId));
        summary.put("permissions", getEffectivePermissions(userId));
        summary.put("userConfig", user.getPermissionConfig());
        
        return summary;
    }
    
    // ===== CLEANUP OPERATIONS =====
    
    public void cleanupExpiredTemporaryPermissions() {
        Query query = Query.query(
            Criteria.where("permissionConfig.temporaryPermissions.expiresAt").lt(LocalDateTime.now())
        );
        
        List<User> users = mongoTemplate.find(query, User.class);
        
        for (User user : users) {
            if (user.getPermissionConfig() != null && user.getPermissionConfig().getTemporaryPermissions() != null) {
                user.getPermissionConfig().getTemporaryPermissions().removeIf(
                    perm -> perm.getExpiresAt().isBefore(LocalDateTime.now())
                );
                mongoTemplate.save(user);
            }
        }
    }
    
    public void cleanupExpiredResourceAccess() {
        Query query = Query.query(
            Criteria.where("permissionConfig.resourceAccesses.expiresAt").lt(LocalDateTime.now())
        );
        
        List<User> users = mongoTemplate.find(query, User.class);
        
        for (User user : users) {
            if (user.getPermissionConfig() != null && user.getPermissionConfig().getResourceAccesses() != null) {
                user.getPermissionConfig().getResourceAccesses().removeIf(
                    access -> access.getExpiresAt() != null && access.getExpiresAt().isBefore(LocalDateTime.now())
                );
                mongoTemplate.save(user);
            }
        }
    }
}