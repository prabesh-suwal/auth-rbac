package com.sb.authenticationrbac.services;

import com.sb.authenticationrbac.dtos.PermissionResult;
import com.sb.authenticationrbac.entities.*;
import com.sb.authenticationrbac.repositories.UserRepository;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
@Service
public class DynamicPermissionEvaluationService {

    private final MongoTemplate mongoTemplate;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final BranchHierarchyService branchHierarchyService;
    private final UserRepository userRepository;

    public DynamicPermissionEvaluationService(MongoTemplate mongoTemplate,
                                              BranchHierarchyService branchHierarchyService, UserRepository userRepository) {
        this.mongoTemplate = mongoTemplate;
        this.branchHierarchyService = branchHierarchyService;
        this.userRepository = userRepository;
    }

    /**
     * Main method to check if user has permission
     */
    public PermissionResult hasPermission(String userId, String permissionName,
                                          String operation, Object resource,
                                          Map<String, Object> context) {

        // Load user and their roles
//        ObjectId objectId = new ObjectId(userId);
//        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
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

        // Check if user has this permission through roles
        if (!hasPermissionThroughRoles(user, permission.getId())) {
            return PermissionResult.denied("Permission not granted through roles");
        }

        // Evaluate all permission conditions
        return evaluatePermissionConditions(user, permission, operation, resource, context);
    }

    private boolean hasPermissionThroughRoles(User user, String permissionId) {
        List<Role> userRoles = mongoTemplate.find(
                Query.query(Criteria.where("name").in(user.getRoleIds()).and("active").is(true)),
                Role.class
        );

        return userRoles.stream()
                .anyMatch(role -> role.getPermissions().contains(permissionId));
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

        switch (branchConfig.getType()) {
            case "ALL_BRANCHES":
                return PermissionResult.allowed("All branches access");

            case "OWN_BRANCH":
                if (userBranchId.equals(resourceBranchId)) {
                    return PermissionResult.allowed("Own branch access");
                }
                return PermissionResult.denied("Access limited to own branch");

            case "SPECIFIC_BRANCHES":
                if (branchConfig.getAllowedBranches().contains(resourceBranchId)) {
                    return PermissionResult.allowed("Specific branch access granted");
                }
                return PermissionResult.denied("Branch not in allowed list");

            case "BRANCH_HIERARCHY":
                if (branchHierarchyService.hasAccessToBranch(userBranchId, resourceBranchId, branchConfig.isIncludeSubBranches())) {
                    return PermissionResult.allowed("Branch hierarchy access");
                }
                return PermissionResult.denied("Branch not in hierarchy");

            default:
                return PermissionResult.denied("Unknown branch access type");
        }
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
            // Implementation would check user-specific limits
        }

        switch (config.getLimitType()) {
            case "FIXED":
                return config.getDefaultLimit();

            case "ROLE_BASED":
                return calculateRoleBasedLimit(user, config.getRoleLimits());

            case "DYNAMIC":
                return evaluateDynamicLimit(user, config.getDynamicLimitExpression());

            default:
                return config.getDefaultLimit();
        }
    }

    private Double calculateRoleBasedLimit(User user, Map<String, Double> roleLimits) {
        List<Role> userRoles = mongoTemplate.find(
                Query.query(Criteria.where("_id").in(user.getRoleIds())),
                Role.class
        );

        return userRoles.stream()
                .map(role -> roleLimits.getOrDefault(role.getName(), 0.0))
                .max(Double::compareTo)
                .orElse(0.0);
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

        LocalDateTime now = LocalDateTime.now();
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
        LocalTime startTime = LocalTime.parse(window.getStartTime(), DateTimeFormatter.ofPattern("HH:mm"));
        LocalTime endTime = LocalTime.parse(window.getEndTime(), DateTimeFormatter.ofPattern("HH:mm"));

        return !currentTime.isBefore(startTime) && !currentTime.isAfter(endTime);
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
                if (resourceConfig.getAllowedResourceIds().contains(resourceId)) {
                    return PermissionResult.allowed("Specific resource access granted");
                }
                return PermissionResult.denied("Resource not in allowed list");

            case "CONDITIONAL":
                boolean b = evaluateResourceConditions(user, resourceConfig.getAccessConditions(), resource, context);
                if (b ) {
                    return PermissionResult.allowed("Conditional resource access granted");
                }else {
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
            String resourceType = resource.getClass().getSimpleName().toUpperCase();

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
            evalContext.setVariable("now", LocalDateTime.now());

            Expression expression = parser.parseExpression(condition);
            Boolean result = expression.getValue(evalContext, Boolean.class);
            return result != null && result;
        } catch (Exception e) {
            // Log error and return false for safety
            return false;
        }
    }

    private boolean evaluateResourceConditions(User user, List<String> conditions, Object resource, Map<String, Object> context) {
        for (String condition : conditions) {
            if (!evaluateSpELCondition(user, condition, resource, context)) {
                return false;
            }
        }
        return true;
    }

    private boolean evaluateTemporaryPermissionConditions(TemporaryPermission tempPerm, Object resource, Map<String, Object> context) {
        // Implement evaluation of temporary permission conditions
        return tempPerm.getConditions() == null || tempPerm.getConditions().isEmpty();
    }

    // Helper methods for extracting data from resources
    private String extractBranchId(Object resource, String fieldPath) {
        // Implementation to extract branch ID using reflection or specific logic
        return null;
    }

    private Double extractAmount(Object resource, Map<String, Object> context, String fieldPath) {
        // Implementation to extract amount from resource or context
        return null;
    }

    private String extractOwnerId(Object resource, String fieldPath) {
        // Implementation to extract owner ID
        return null;
    }

    private String extractResourceId(Object resource) {
        // Implementation to extract resource ID
        return null;
    }

    private Branch getBranch(String branchId) {
        return mongoTemplate.findById(branchId, Branch.class);
    }

    private void logPermissionEvaluation(String userId, String permissionId, String operation,
                                         boolean allowed, String reason, Map<String, Object> context) {
        PermissionEvaluation evaluation = new PermissionEvaluation();
        evaluation.setUserId(userId);
        evaluation.setPermissionId(permissionId);
        evaluation.setOperation(operation);
        evaluation.setAllowed(allowed);
        evaluation.setReason(reason);
        evaluation.setContext(context);
        evaluation.setEvaluatedAt(LocalDateTime.now());

        mongoTemplate.save(evaluation);
    }
}
