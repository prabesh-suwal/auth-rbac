package com.sb.authenticationrbac.services;

import com.sb.authenticationrbac.dtos.PermissionResult;
import com.sb.authenticationrbac.entities.*;
import com.sb.authenticationrbac.repositories.PermissionRepository;
import com.sb.authenticationrbac.repositories.RoleRepository;
import com.sb.authenticationrbac.repositories.UserRepository;
import com.sb.authenticationrbac.role.dto.RoleConfiguration;
import com.sb.authenticationrbac.security.SecurityContextUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DynamicPermissionEvaluationService {

    private final MongoTemplate mongoTemplate;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final BranchHierarchyService branchHierarchyService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PermissionPerformanceMonitoringService performanceMonitoringService;

    public DynamicPermissionEvaluationService(MongoTemplate mongoTemplate,
                                              BranchHierarchyService branchHierarchyService, 
                                              UserRepository userRepository, 
                                              RoleRepository roleRepository, 
                                              PermissionRepository permissionRepository,
                                              PermissionPerformanceMonitoringService performanceMonitoringService) {
        this.mongoTemplate = mongoTemplate;
        this.branchHierarchyService = branchHierarchyService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.performanceMonitoringService = performanceMonitoringService;
    }

    /**
     * Check permission for the current authenticated user
     */
    public PermissionResult hasPermission(String permissionName, String operation, Object resource, Map<String, Object> context) {
        String currentUserId = getCurrentUserId();
        return hasPermission(currentUserId, permissionName, operation, resource, context);
    }

    /**
     * Main method to check if user has permission
     */
    public PermissionResult hasPermission(String userId, String permissionName,
                                          String operation, Object resource,
                                          Map<String, Object> context) {
        
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        boolean fromCache = false;
        PermissionResult result = null;
        
        try {
            // Try to get from cache first
            String cacheKey = userId + "_" + permissionName + "_" + operation;
            result = getCachedPermissionResult(cacheKey, userId, permissionName, operation, resource, context);
            
            if (result != null) {
                fromCache = true;
            } else {
                // Evaluate permission
                result = evaluatePermissionInternal(userId, permissionName, operation, resource, context);
            }
            
        } catch (Exception e) {
            result = PermissionResult.denied("Permission evaluation failed: " + e.getMessage());
        } finally {
            stopWatch.stop();
            
            // Record performance metrics
            if (result != null) {
                performanceMonitoringService.recordPermissionEvaluation(
                    permissionName, 
                    userId, 
                    stopWatch.getTotalTimeMillis(), 
                    result.isAllowed(), 
                    fromCache
                );
            }
        }
        
        return result != null ? result : PermissionResult.denied("Unknown error occurred");
    }

    /**
     * Get the current authenticated user's ID from SecurityContext
     * 
     * @return current user ID
     * @throws IllegalStateException if no user is authenticated
     */
    private String getCurrentUserId() {
        return SecurityContextUtils.getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException("No authenticated user found in security context"));
    }

    /**
     * Get the current authenticated user's branch ID from SecurityContext
     * 
     * @return current user's branch ID
     */
    private Optional<String> getCurrentUserBranchId() {
        return SecurityContextUtils.getCurrentUserBranchId();
    }

    /**
     * Check if the current user has a specific authority
     * 
     * @param authority the authority to check
     * @return true if the user has the authority
     */
    private boolean currentUserHasAuthority(String authority) {
        return SecurityContextUtils.hasAuthority(authority);
    }

    @Cacheable(value = "permissionEvaluations", key = "#cacheKey")
    public PermissionResult getCachedPermissionResult(String cacheKey, String userId, String permissionName,
                                                      String operation, Object resource, Map<String, Object> context) {
        // This method will only be called if not in cache
        // Return null to indicate cache miss
        return null;
    }

    private PermissionResult evaluatePermissionInternal(String userId, String permissionName,
                                                       String operation, Object resource,
                                                       Map<String, Object> context) {
        // Load user and their roles
        User user = mongoTemplate.findById(userId, User.class);
        if (user == null || !user.isActive()) {
            return PermissionResult.denied("User not found or inactive");
        }

        // Load permission configuration
        Permission permission = mongoTemplate.findOne(
                Query.query(Criteria.where("name").is(permissionName).and("active").is(true)),
                Permission.class
        );

        if (permission == null) {
            return PermissionResult.denied("Permission not found");
        }

        List<Role> userRoles = this.roleRepository.findByIdInAndActive(user.getRoleIds(), true);
        Permission superAdmin = this.permissionRepository.findByName("SUPER_ADMIN_ACCESS").orElse(new Permission());
        Set<String> permissionIds = userRoles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getId)
                .collect(Collectors.toSet());

        if (permissionIds.contains(superAdmin.getId())){
            return PermissionResult.allowed("Super admin permission");
        }

        // Check if user has this permission through roles
        if (!hasPermissionThroughRoles(user, permission.getId())) {
            return PermissionResult.denied("Permission not granted through roles");
        }

        // Evaluate all permission conditions
        return evaluatePermissionConditions(user, permission, operation, resource, context);
    }

    private boolean hasPermissionThroughRoles(User user, String permissionId) {
        List<Role> userRoles = this.roleRepository.findByIdInAndActive(user.getRoleIds(), true);
        Permission superAdmin = this.permissionRepository.findByName("SUPER_ADMIN_ACCESS").orElse(new Permission());
        Set<String> permissionIds = userRoles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getId)
                .collect(Collectors.toSet());

        return permissionIds.contains(permissionId) || permissionIds.contains(superAdmin.getId());
    }

    private PermissionResult evaluatePermissionConditions(User user, Permission permission,
                                                          String operation, Object resource,
                                                          Map<String, Object> context) {

        PermissionConfig config = permission.getConfig();
        if (config == null) {
            return PermissionResult.allowed("No additional conditions");
        }

        // Check branch access
        PermissionResult branchResult = evaluateBranchAccess(user, config.getBranchAccess(), resource, context);
        if (!branchResult.isAllowed()) {
            return branchResult;
        }

        // Check amount limits
        PermissionResult amountResult = evaluateAmountLimit(user, config.getAmountLimit(), resource, context);
        if (!amountResult.isAllowed()) {
            return amountResult;
        }

        // Check time-based access
        PermissionResult timeResult = evaluateTimeAccess(user, config.getTimeAccess(), context);
        if (!timeResult.isAllowed()) {
            return timeResult;
        }

        // Check resource-specific access
        PermissionResult resourceResult = evaluateResourceAccess(user, config.getResourceAccess(), resource, context);
        if (!resourceResult.isAllowed()) {
            return resourceResult;
        }

        // Evaluate custom conditions
        PermissionResult conditionResult = evaluateCustomConditions(user, config.getConditions(), resource, context);
        if (!conditionResult.isAllowed()) {
            return conditionResult;
        }

        // Check validation rules
        PermissionResult validationResult = evaluateValidationRules(user, config.getValidationRules(), resource, context);
        if (!validationResult.isAllowed()) {
            return validationResult;
        }

        // Check user-specific overrides
        PermissionResult userOverrideResult = evaluateUserOverrides(user, permission.getName(), resource, context);
        if (!userOverrideResult.isAllowed()) {
            return userOverrideResult;
        }

        // Log successful evaluation
        logPermissionEvaluation(user.getId(), permission.getId(), operation, true, "All conditions passed", context);

        return PermissionResult.allowed("All conditions satisfied");
    }

    private PermissionResult evaluateBranchAccess(User user, BranchAccessConfig branchConfig,
                                                  Object resource, Map<String, Object> context) {
        if (branchConfig == null || branchConfig.getType() == null) {
            return PermissionResult.allowed("No branch restrictions");
        }

        String userBranchId = user.getBranchId();
        String resourceBranchId = extractBranchId(resource, branchConfig.getBranchFieldPath());

        // HIERARCHY 1: Check user-specific branch access overrides (highest priority)
        PermissionResult userOverrideResult = checkUserSpecificBranchAccess(user, userBranchId, resourceBranchId, context);
        if (userOverrideResult != null) {
            return userOverrideResult;
        }

        // HIERARCHY 2: Check role-level branch access configuration (medium priority)
        PermissionResult roleLevelResult = checkRoleLevelBranchAccess(user, userBranchId, resourceBranchId, branchConfig, context);
        if (roleLevelResult != null) {
            return roleLevelResult;
        }

        // HIERARCHY 3: Apply permission-level branch access configuration (lowest priority)
        return applyPermissionLevelBranchAccess(userBranchId, resourceBranchId, branchConfig);
    }

    /**
     * HIERARCHY 1: Check user-specific branch access overrides
     * Highest priority - user-specific configurations override everything
     */
    private PermissionResult checkUserSpecificBranchAccess(User user, String userBranchId, 
                                                          String resourceBranchId, Map<String, Object> context) {
        if (user.getPermissionConfig() == null) {
            return null; // No user-specific overrides
        }

        // Check if user has specific branch access grants
        List<ResourceAccess> resourceAccesses = user.getPermissionConfig().getResourceAccesses();
        if (resourceAccesses != null) {
            for (ResourceAccess access : resourceAccesses) {
                if ("BRANCH".equals(access.getResourceType()) && 
                    resourceBranchId.equals(access.getResourceId())) {
                    
                    if ("GRANT".equals(access.getAccessType())) {
                        return PermissionResult.allowed("User-specific branch access grant");
                    } else if ("DENY".equals(access.getAccessType())) {
                        return PermissionResult.denied("User-specific branch access denial");
                    }
                }
            }
        }

        // Check branch access overrides in user permission config
        if (user.getPermissionConfig().getBranchAccessOverride() != null) {
            BranchAccessConfig branchOverride = user.getPermissionConfig().getBranchAccessOverride();
            
            // Check if user has specific override configuration
            if (branchOverride.getType() != null) {
                switch (branchOverride.getType()) {
                    case "ALL_BRANCHES":
                        return PermissionResult.allowed("User-specific branch override: ALL_BRANCHES");
                    case "OWN_BRANCH":
                        if (userBranchId.equals(resourceBranchId)) {
                            return PermissionResult.allowed("User-specific branch override: OWN_BRANCH");
                        }
                        return PermissionResult.denied("User-specific branch override: OWN_BRANCH restriction");
                    case "SPECIFIC_BRANCHES":
                        if (branchOverride.getAllowedBranches() != null && 
                            branchOverride.getAllowedBranches().contains(resourceBranchId)) {
                            return PermissionResult.allowed("User-specific branch override: SPECIFIC_BRANCHES");
                        }
                        return PermissionResult.denied("User-specific branch override: Branch not in allowed list");
                    case "INHERIT":
                        // Continue to role-level check
                        break;
                }
            }
        }

        return null; // No user-specific override found, continue to next hierarchy
    }

    /**
     * HIERARCHY 2: Check role-level branch access configuration
     * Medium priority - role configurations can override permission restrictions
     */
    private PermissionResult checkRoleLevelBranchAccess(User user, String userBranchId, String resourceBranchId,
                                                       BranchAccessConfig branchConfig, Map<String, Object> context) {
        List<Role> userRoles = roleRepository.findByIdInAndActive(user.getRoleIds(), true);
        
        // Check each role's branch restrictions (highest role privilege wins)
        RoleBranchAccessResult roleBranchAccess = determineRoleBranchAccess(userRoles, userBranchId, resourceBranchId);
        
        if (roleBranchAccess.hasAccess()) {
            return PermissionResult.allowed(roleBranchAccess.getReason());
        } else if (roleBranchAccess.isExplicitDeny()) {
            return PermissionResult.denied(roleBranchAccess.getReason());
        }

        return null; // No role-level override, continue to permission-level check
    }

    /**
     * HIERARCHY 3: Apply permission-level branch access configuration
     * Lowest priority - default permission-based access control
     */
    private PermissionResult applyPermissionLevelBranchAccess(String userBranchId, String resourceBranchId, 
                                                             BranchAccessConfig branchConfig) {
        switch (branchConfig.getType()) {
            case "ALL_BRANCHES":
                return PermissionResult.allowed("Permission-level: All branches access");

            case "OWN_BRANCH":
                if (userBranchId.equals(resourceBranchId)) {
                    return PermissionResult.allowed("Permission-level: Own branch access");
                }
                return PermissionResult.denied("Permission-level: Access limited to own branch");

            case "SPECIFIC_BRANCHES":
                if (branchConfig.getAllowedBranches() != null && branchConfig.getAllowedBranches().contains(resourceBranchId)) {
                    return PermissionResult.allowed("Permission-level: Specific branch access granted");
                }
                return PermissionResult.denied("Permission-level: Branch not in allowed list");

            case "BRANCH_HIERARCHY":
                if (branchHierarchyService.hasAccessToBranch(userBranchId, resourceBranchId, branchConfig.isIncludeSubBranches())) {
                    return PermissionResult.allowed("Permission-level: Branch hierarchy access");
                }
                return PermissionResult.denied("Permission-level: Branch not in hierarchy");

            default:
                return PermissionResult.denied("Permission-level: Unknown branch access type");
        }
    }

    /**
     * Determine the effective branch access based on all user roles
     * Implements role hierarchy where higher privileges override lower ones
     */
    private RoleBranchAccessResult determineRoleBranchAccess(List<Role> userRoles, String userBranchId, String resourceBranchId) {
        boolean hasAllBranchesAccess = false;
        boolean hasSubordinateAccess = false;
        boolean hasCrossBranchView = false;
        String highestPrivilegeRole = null;
        
        for (Role role : userRoles) {
            if (role.getConfiguration() == null || role.getConfiguration().getBranchRestrictions() == null) {
                continue;
            }
            
            RoleConfiguration.BranchRestrictions restrictions = role.getConfiguration().getBranchRestrictions();
            String restrictionType = restrictions.getType();
            boolean allowCrossBranch = restrictions.isAllowCrossBranchView();
            
            switch (restrictionType) {
                case "ALL_BRANCHES":
                    hasAllBranchesAccess = true;
                    highestPrivilegeRole = role.getName();
                    break;
                    
                case "OWN_BRANCH_AND_SUBORDINATES":
                    if (!hasAllBranchesAccess) {
                        hasSubordinateAccess = true;
                        if (allowCrossBranch) {
                            hasCrossBranchView = true;
                        }
                        if (highestPrivilegeRole == null) {
                            highestPrivilegeRole = role.getName();
                        }
                    }
                    break;
                    
                case "OWN_BRANCH_ONLY":
                    if (!hasAllBranchesAccess && !hasSubordinateAccess && allowCrossBranch) {
                        hasCrossBranchView = true;
                        if (highestPrivilegeRole == null) {
                            highestPrivilegeRole = role.getName();
                        }
                    }
                    break;
            }
        }
        
        // Determine access based on highest privilege
        if (hasAllBranchesAccess) {
            return new RoleBranchAccessResult(true, false, 
                String.format("Role-level: ALL_BRANCHES access via %s role", highestPrivilegeRole));
        }
        
        if (hasSubordinateAccess) {
            // Check if target branch is in hierarchy
            if (userBranchId.equals(resourceBranchId) || 
                branchHierarchyService.hasAccessToBranch(userBranchId, resourceBranchId, true)) {
                return new RoleBranchAccessResult(true, false,
                    String.format("Role-level: Subordinate branch access via %s role", highestPrivilegeRole));
            }
            
            // If cross-branch view is allowed, grant access
            if (hasCrossBranchView) {
                return new RoleBranchAccessResult(true, false,
                    String.format("Role-level: Cross-branch access via %s role", highestPrivilegeRole));
            }
        }
        
        if (hasCrossBranchView && !userBranchId.equals(resourceBranchId)) {
            return new RoleBranchAccessResult(true, false,
                String.format("Role-level: Cross-branch view access via %s role", highestPrivilegeRole));
        }
        
        // No role-level access granted
        return new RoleBranchAccessResult(false, false, "No role-level branch access");
    }

    /**
     * Helper class to encapsulate role-based branch access results
     */
    private static class RoleBranchAccessResult {
        private final boolean hasAccess;
        private final boolean explicitDeny;
        private final String reason;
        
        public RoleBranchAccessResult(boolean hasAccess, boolean explicitDeny, String reason) {
            this.hasAccess = hasAccess;
            this.explicitDeny = explicitDeny;
            this.reason = reason;
        }
        
        public boolean hasAccess() { return hasAccess; }
        public boolean isExplicitDeny() { return explicitDeny; }
        public String getReason() { return reason; }
    }

    private PermissionResult evaluateAmountLimit(User user, AmountLimitConfig amountConfig,
                                                 Object resource, Map<String, Object> context) {
        if (amountConfig == null || !amountConfig.isEnabled()) {
            return PermissionResult.allowed("No amount limits");
        }

        Double amount = extractAmount(resource, context, amountConfig.getAmountFieldPath());
        if (amount == null) {
            return PermissionResult.allowed("No amount to check");
        }

        Double limit = calculateAmountLimit(user, amountConfig);

        if (amount <= limit) {
            return PermissionResult.allowed("Amount within limit");
        }

        return PermissionResult.denied(String.format("Amount %.2f exceeds limit %.2f", amount, limit));
    }

    private Double calculateAmountLimit(User user, AmountLimitConfig config) {
        // Check user-specific override first
        if (user.getPermissionConfig() != null && user.getPermissionConfig().getAmountLimitOverrides() != null) {
            // Get the highest user-specific limit
            Double userLimit = user.getPermissionConfig().getAmountLimitOverrides().values().stream()
                    .max(Double::compareTo)
                    .orElse(null);
            if (userLimit != null) {
                return userLimit;
            }
        }

        switch (config.getLimitType()) {
            case "FIXED":
                return config.getDefaultLimit();

            case "ROLE_BASED":
                return calculateRoleBasedLimit(user, config.getRoleLimits());

            case "USER_SPECIFIC":
                return getUserSpecificLimit(user, config);

            case "DYNAMIC":
                return evaluateDynamicLimit(user, config.getDynamicLimitExpression());

            default:
                return config.getDefaultLimit();
        }
    }

    private Double calculateRoleBasedLimit(User user, Map<String, Double> roleLimits) {
        if (roleLimits == null) {
            return 0.0;
        }

        List<Role> userRoles = mongoTemplate.find(
                Query.query(Criteria.where("_id").in(user.getRoleIds())),
                Role.class
        );

        return userRoles.stream()
                .map(role -> roleLimits.getOrDefault(role.getName(), 0.0))
                .max(Double::compareTo)
                .orElse(0.0);
    }

    private Double getUserSpecificLimit(User user, AmountLimitConfig config) {
        if (user.getPermissionConfig() != null && user.getPermissionConfig().getAmountLimitOverrides() != null) {
            return user.getPermissionConfig().getAmountLimitOverrides().values().stream()
                    .max(Double::compareTo)
                    .orElse(config.getDefaultLimit());
        }
        return config.getDefaultLimit();
    }

    private Double evaluateDynamicLimit(User user, String expression) {
        if (expression == null || expression.isEmpty()) {
            return 0.0;
        }

        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariable("user", user);
            context.setVariable("userBranch", getBranch(user.getBranchId()));

            Expression expr = parser.parseExpression(expression);
            Number result = expr.getValue(context, Number.class);
            return result != null ? result.doubleValue() : 0.0;
        } catch (Exception e) {
            return 0.0; // Safe fallback
        }
    }

    private PermissionResult evaluateTimeAccess(User user, TimeAccessConfig timeConfig,
                                                Map<String, Object> context) {
        if (timeConfig == null || !timeConfig.isEnabled()) {
            return PermissionResult.allowed("No time restrictions");
        }

        ZoneId nepalTimeZone = ZoneId.of("Asia/Kathmandu");
        LocalDateTime now = LocalDateTime.now(nepalTimeZone);
        String currentDay = now.getDayOfWeek().name();
        LocalTime currentTime = now.toLocalTime();

        // Check allowed days
        if (timeConfig.getAllowedDays() != null && !timeConfig.getAllowedDays().contains(currentDay)) {
            return PermissionResult.denied("Access not allowed on " + currentDay);
        }

        // Check time windows
        if (timeConfig.getAllowedTimeWindows() != null) {
            boolean withinWindow = timeConfig.getAllowedTimeWindows().stream()
                    .anyMatch(window -> isWithinTimeWindow(currentTime, window));

            if (!withinWindow) {
                return PermissionResult.denied("Access not allowed at current time");
            }
        }

        return PermissionResult.allowed("Time access granted");
    }

    private boolean isWithinTimeWindow(LocalTime currentTime, TimeWindow window) {
        try {
            LocalTime startTime = LocalTime.parse(window.getStartTime(), DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime endTime = LocalTime.parse(window.getEndTime(), DateTimeFormatter.ofPattern("HH:mm"));

            return !currentTime.isBefore(startTime) && !currentTime.isAfter(endTime);
        } catch (Exception e) {
            return false; // Safe fallback
        }
    }

    private PermissionResult evaluateResourceAccess(User user, ResourceAccessConfig resourceConfig,
                                                    Object resource, Map<String, Object> context) {
        if (resourceConfig == null) {
            return PermissionResult.allowed("No resource restrictions");
        }

        switch (resourceConfig.getAccessType()) {
            case "ALL":
                return PermissionResult.allowed("All resources access");

            case "OWN":
                String ownerId = extractOwnerId(resource, resourceConfig.getOwnershipFieldPath());
                if (user.getId().equals(ownerId)) {
                    return PermissionResult.allowed("Own resource access");
                }
                return PermissionResult.denied("Access limited to own resources");

            case "SPECIFIC":
                String resourceId = extractResourceId(resource);
                if (resourceConfig.getAllowedResourceIds() != null && resourceConfig.getAllowedResourceIds().contains(resourceId)) {
                    return PermissionResult.allowed("Specific resource access granted");
                }
                return PermissionResult.denied("Resource not in allowed list");

            case "CONDITIONAL":
                boolean conditionMet = evaluateResourceConditions(user, resourceConfig.getAccessConditions(), resource, context);
                if (conditionMet) {
                    return PermissionResult.allowed("Conditional resource access granted");
                } else {
                    return PermissionResult.denied("Conditional resource access denied");
                }

            default:
                return PermissionResult.denied("Unknown resource access type");
        }
    }

    private PermissionResult evaluateCustomConditions(User user, List<String> conditions,
                                                      Object resource, Map<String, Object> context) {
        if (conditions == null || conditions.isEmpty()) {
            return PermissionResult.allowed("No custom conditions");
        }

        for (String condition : conditions) {
            if (!evaluateSpELCondition(user, condition, resource, context)) {
                return PermissionResult.denied("Custom condition failed: " + condition);
            }
        }

        return PermissionResult.allowed("All custom conditions passed");
    }

    private PermissionResult evaluateValidationRules(User user, List<ValidationRule> rules,
                                                     Object resource, Map<String, Object> context) {
        if (rules == null || rules.isEmpty()) {
            return PermissionResult.allowed("No validation rules");
        }

        // Sort by priority
        List<ValidationRule> sortedRules = rules.stream()
                .sorted(Comparator.comparingInt(ValidationRule::getPriority))
                .collect(Collectors.toList());

        for (ValidationRule rule : sortedRules) {
            if (!evaluateSpELCondition(user, rule.getCondition(), resource, context)) {
                return PermissionResult.denied(rule.getErrorMessage());
            }
        }

        return PermissionResult.allowed("All validation rules passed");
    }

    private PermissionResult evaluateUserOverrides(User user, String permissionName,
                                                   Object resource, Map<String, Object> context) {
        if (user.getPermissionConfig() == null) {
            return PermissionResult.allowed("No user overrides");
        }

        // Check temporary permissions
        List<TemporaryPermission> tempPerms = user.getPermissionConfig().getTemporaryPermissions();
        if (tempPerms != null) {
            for (TemporaryPermission tempPerm : tempPerms) {
                if (tempPerm.getPermissionId().equals(permissionName) &&
                        tempPerm.getExpiresAt().isAfter(LocalDateTime.now())) {

                    // Check temporary permission conditions
                    if (evaluateTemporaryPermissionConditions(tempPerm, resource, context)) {
                        return PermissionResult.allowed("Temporary permission granted");
                    }
                }
            }
        }

        // Check resource-specific access
        List<ResourceAccess> resourceAccesses = user.getPermissionConfig().getResourceAccesses();
        if (resourceAccesses != null) {
            String resourceId = extractResourceId(resource);
            String resourceType = resource != null ? resource.getClass().getSimpleName().toUpperCase() : null;

            for (ResourceAccess access : resourceAccesses) {
                if (access.getResourceType().equals(resourceType) &&
                        access.getResourceId().equals(resourceId)) {

                    if ("DENY".equals(access.getAccessType())) {
                        return PermissionResult.denied("Explicit resource access denial");
                    } else if ("GRANT".equals(access.getAccessType())) {
                        return PermissionResult.allowed("Explicit resource access grant");
                    }
                }
            }
        }

        return PermissionResult.allowed("No applicable user overrides");
    }

    private boolean evaluateSpELCondition(User user, String condition, Object resource, Map<String, Object> context) {
        try {
            StandardEvaluationContext evalContext = new StandardEvaluationContext();
            evalContext.setVariable("user", user);
            evalContext.setVariable("resource", resource);
            evalContext.setVariable("context", context);
            evalContext.setVariable("userBranch", getBranch(user.getBranchId()));
            ZoneId nepalTimeZone = ZoneId.of("Asia/Kathmandu");
            evalContext.setVariable("now", LocalDateTime.now(nepalTimeZone));

            Expression expression = parser.parseExpression(condition);
            Boolean result = expression.getValue(evalContext, Boolean.class);
            return result != null && result;
        } catch (Exception e) {
            // Log error and return false for safety
            return false;
        }
    }

    private boolean evaluateResourceConditions(User user, List<String> conditions, Object resource, Map<String, Object> context) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }
        
        for (String condition : conditions) {
            if (!evaluateSpELCondition(user, condition, resource, context)) {
                return false;
            }
        }
        return true;
    }

    private boolean evaluateTemporaryPermissionConditions(TemporaryPermission tempPerm, Object resource, Map<String, Object> context) {
        if (tempPerm.getConditions() == null || tempPerm.getConditions().isEmpty()) {
            return true;
        }

        // Evaluate each condition in the temporary permission
        for (Map.Entry<String, Object> entry : tempPerm.getConditions().entrySet()) {
            String conditionKey = entry.getKey();
            Object expectedValue = entry.getValue();
            
            // Extract actual value from resource or context
            Object actualValue = extractFieldValue(resource, conditionKey);
            if (actualValue == null && context != null) {
                actualValue = context.get(conditionKey);
            }
            
            if (!Objects.equals(expectedValue, actualValue)) {
                return false;
            }
        }
        return true;
    }

    // Helper methods for extracting data from resources
    private String extractBranchId(Object resource, String fieldPath) {
        if (resource == null || fieldPath == null) {
            return null;
        }
        
        Object value = extractFieldValue(resource, fieldPath);
        return value != null ? value.toString() : null;
    }

    private Double extractAmount(Object resource, Map<String, Object> context, String fieldPath) {
        // First try to get from context
        if (context != null && context.containsKey("amount")) {
            Object amount = context.get("amount");
            return convertToDouble(amount);
        }
        
        // Then try to extract from resource
        if (resource != null && fieldPath != null) {
            Object value = extractFieldValue(resource, fieldPath);
            return convertToDouble(value);
        }
        
        return null;
    }

    private String extractOwnerId(Object resource, String fieldPath) {
        if (resource == null || fieldPath == null) {
            return null;
        }
        
        Object value = extractFieldValue(resource, fieldPath);
        return value != null ? value.toString() : null;
    }

    private String extractResourceId(Object resource) {
        if (resource == null) {
            return null;
        }
        
        // Try common ID field names
        String[] idFields = {"id", "_id", "resourceId"};
        for (String fieldName : idFields) {
            Object value = extractFieldValue(resource, fieldName);
            if (value != null) {
                return value.toString();
            }
        }
        
        return null;
    }

    /**
     * Generic field value extraction using reflection and dot notation
     */
    private Object extractFieldValue(Object object, String fieldPath) {
        if (object == null || !StringUtils.hasText(fieldPath)) {
            return null;
        }

        try {
            // Handle Map objects
            if (object instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) object;
                String[] pathParts = fieldPath.split("\\.");
                Object current = map;
                
                for (String part : pathParts) {
                    if (current instanceof Map) {
                        current = ((Map<?, ?>) current).get(part);
                    } else {
                        return null;
                    }
                }
                return current;
            }

            // Handle regular objects using reflection
            String[] pathParts = fieldPath.split("\\.");
            Object current = object;
            
            for (String part : pathParts) {
                if (current == null) {
                    return null;
                }
                
                Class<?> currentClass = current.getClass();
                Field field = findField(currentClass, part);
                
                if (field != null) {
                    field.setAccessible(true);
                    current = field.get(current);
                } else {
                    return null;
                }
            }
            
            return current;
        } catch (Exception e) {
            return null;
        }
    }

    private Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    private Double convertToDouble(Object value) {
        if (value == null) {
            return null;
        }
        
        try {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else if (value instanceof String) {
                return Double.parseDouble((String) value);
            } else if (value instanceof BigDecimal) {
                return ((BigDecimal) value).doubleValue();
            }
        } catch (NumberFormatException e) {
            // Ignore and return null
        }
        
        return null;
    }

    @Cacheable(value = "branches", key = "#branchId")
    private Branch getBranch(String branchId) {
        return mongoTemplate.findById(branchId, Branch.class);
    }

    private void logPermissionEvaluation(String userId, String permissionId, String operation,
                                         boolean allowed, String reason, Map<String, Object> context) {
        try {
            PermissionEvaluation evaluation = new PermissionEvaluation();
            evaluation.setUserId(userId);
            evaluation.setPermissionId(permissionId);
            evaluation.setOperation(operation);
            evaluation.setAllowed(allowed);
            evaluation.setReason(reason);
            evaluation.setContext(context);
            ZoneId nepalTimeZone = ZoneId.of("Asia/Kathmandu");
            evaluation.setEvaluatedAt(LocalDateTime.now(nepalTimeZone));

            mongoTemplate.save(evaluation);
        } catch (Exception e) {
            // Log error but don't fail the permission evaluation
        }
    }
}
