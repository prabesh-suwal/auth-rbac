//package com.sb.authenticationrbac.services;
//
//import com.sb.authenticationrbac.dtos.PermissionResult;
//import com.sb.authenticationrbac.entities.*;
//import com.sb.authenticationrbac.repositories.UserRepository; // Though not directly used by service, good for completeness if it were
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockedStatic;
//import org.mockito.Mockito;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.mongodb.core.MongoTemplate;
//import org.springframework.data.mongodb.core.query.Query;
//
//import java.time.LocalDateTime;
//import java.time.LocalTime;
//import java.time.ZoneId;
//import java.util.*;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.lenient;
//import static org.mockito.Mockito.when;
//
//@ExtendWith(MockitoExtension.class)
//class DynamicPermissionEvaluationServiceTest {
//
//    @Mock
//    private MongoTemplate mongoTemplate;
//
//    @Mock
//    private UserRepository userRepository; // Mocked for completeness, though service uses mongoTemplate for User
//
//    @Mock
//    private BranchHierarchyService branchHierarchyService;
//
//    @InjectMocks
//    private DynamicPermissionEvaluationService evaluationService;
//
//    private static final ZoneId NPT = ZoneId.of("Asia/Kathmandu");
//    private static final String TEST_USER_ID = "user123";
//    private static final String TEST_PERMISSION_NAME = "VIEW_DOCUMENT";
//    private static final String TEST_PERMISSION_ID = "perm_view_doc_id";
//    private static final String TEST_ROLE_ID = "role_viewer_id";
//    private static final String TEST_ROLE_NAME = "VIEWER_ROLE";
//    private static final String TEST_BRANCH_ID = "branch001";
//    private static final String OTHER_BRANCH_ID = "branch002";
//
//
//    @BeforeEach
//    void setUp() {
//        // Common lenient mocks can go here if applicable across many tests
//        // For example, if a User is always needed:
//        // User mockUser = createMockUser(TEST_USER_ID, TEST_ROLE_NAME, TEST_BRANCH_ID, true);
//        // lenient().when(mongoTemplate.findById(eq(TEST_USER_ID), eq(User.class))).thenReturn(mockUser);
//    }
//
//    private User createMockUser(String userId, String roleName, String branchId, boolean isActive) {
//        User user = new User();
//        user.setId(userId);
//        user.setUsername("testuser");
//        user.setRoleIds(Collections.singletonList(roleName)); // Using role name for simplicity in mock setup
//        user.setBranchId(branchId);
//        user.setActive(isActive);
//        user.setPermissionConfig(new UserPermissionConfig()); // Initialize to avoid nulls
//        return user;
//    }
//
//    private Role createMockRole(String roleName, String permissionId) {
//        Role role = new Role();
//        role.setId(TEST_ROLE_ID); // Keep ID consistent or make it a param
//        role.setName(roleName);
//        role.setPermissions(permissionId != null ? Collections.singletonList(permissionId) : Collections.emptyList());
//        role.setActive(true);
//        return role;
//    }
//
//    private Permission createMockPermission(String permissionName, String permissionId, boolean isActive, PermissionConfig config) {
//        Permission permission = new Permission();
//        permission.setName(permissionName);
//        permission.setId(permissionId);
//        permission.setActive(isActive);
//        permission.setConfig(config);
//        return permission;
//    }
//
//    // --- Basic Positive Case ---
//    @Test
//    void hasPermission_UserHasDirectPermission_NoConditions_ShouldAllow() {
//        // Arrange
//        User user = createMockUser(TEST_USER_ID, TEST_ROLE_NAME, TEST_BRANCH_ID, true);
//        Role role = createMockRole(TEST_ROLE_NAME, TEST_PERMISSION_ID);
//        Permission permission = createMockPermission(TEST_PERMISSION_NAME, TEST_PERMISSION_ID, true, null); // No config
//
//        when(mongoTemplate.findById(eq(TEST_USER_ID), eq(User.class))).thenReturn(user);
//        when(mongoTemplate.find(any(Query.class), eq(Role.class))).thenReturn(Collections.singletonList(role));
//        when(mongoTemplate.findOne(any(Query.class), eq(Permission.class))).thenReturn(permission);
//
//        // Act
//        PermissionResult result = evaluationService.hasPermission(TEST_USER_ID, TEST_PERMISSION_NAME, "READ", null, Collections.emptyMap());
//
//        // Assert
//        assertTrue(result.isAllowed(), "Permission should be allowed: " + result.getReason());
//        assertEquals("All conditions satisfied", result.getReason());
//    }
//
//    // --- Basic Negative Cases ---
//    @Test
//    void hasPermission_UserNotFound_ShouldDeny() {
//        // Arrange
//        when(mongoTemplate.findById(eq(TEST_USER_ID), eq(User.class))).thenReturn(null);
//
//        // Act
//        PermissionResult result = evaluationService.hasPermission(TEST_USER_ID, TEST_PERMISSION_NAME, "READ", null, Collections.emptyMap());
//
//        // Assert
//        assertFalse(result.isAllowed());
//        assertEquals("User not found or inactive", result.getReason());
//    }
//
//    @Test
//    void hasPermission_UserInactive_ShouldDeny() {
//        // Arrange
//        User user = createMockUser(TEST_USER_ID, TEST_ROLE_NAME, TEST_BRANCH_ID, false); // Inactive user
//        when(mongoTemplate.findById(eq(TEST_USER_ID), eq(User.class))).thenReturn(user);
//
//        // Act
//        PermissionResult result = evaluationService.hasPermission(TEST_USER_ID, TEST_PERMISSION_NAME, "READ", null, Collections.emptyMap());
//
//        // Assert
//        assertFalse(result.isAllowed());
//        assertEquals("User not found or inactive", result.getReason());
//    }
//
//    @Test
//    void hasPermission_PermissionNotFound_ShouldDeny() {
//        // Arrange
//        User user = createMockUser(TEST_USER_ID, TEST_ROLE_NAME, TEST_BRANCH_ID, true);
//        when(mongoTemplate.findById(eq(TEST_USER_ID), eq(User.class))).thenReturn(user);
//        when(mongoTemplate.findOne(any(Query.class), eq(Permission.class))).thenReturn(null); // Permission not found
//
//        // Act
//        PermissionResult result = evaluationService.hasPermission(TEST_USER_ID, TEST_PERMISSION_NAME, "READ", null, Collections.emptyMap());
//
//        // Assert
//        assertFalse(result.isAllowed());
//        assertEquals("Permission not found", result.getReason());
//    }
//
//    @Test
//    void hasPermission_PermissionInactive_ShouldDeny() {
//        // Arrange
//        User user = createMockUser(TEST_USER_ID, TEST_ROLE_NAME, TEST_BRANCH_ID, true);
//        Permission permission = createMockPermission(TEST_PERMISSION_NAME, TEST_PERMISSION_ID, false, null); // Inactive permission
//
//        when(mongoTemplate.findById(eq(TEST_USER_ID), eq(User.class))).thenReturn(user);
//        when(mongoTemplate.findOne(any(Query.class), eq(Permission.class))).thenReturn(permission);
//
//        // Act
//        PermissionResult result = evaluationService.hasPermission(TEST_USER_ID, TEST_PERMISSION_NAME, "READ", null, Collections.emptyMap());
//
//        // Assert
//        assertFalse(result.isAllowed());
//        // Note: The service currently doesn't explicitly check permission.isActive() *before* checking roles.
//        // If it did, the message might be "Permission inactive". Current logic would lead to "Permission not granted through roles"
//        // if no other active permission definition with the same name exists and is matched.
//        // For this test, assuming it proceeds to role check:
//         assertEquals("Permission not granted through roles", result.getReason());
//    }
//
//
//    @Test
//    void hasPermission_UserDoesNotHavePermissionInRoles_ShouldDeny() {
//        // Arrange
//        User user = createMockUser(TEST_USER_ID, TEST_ROLE_NAME, TEST_BRANCH_ID, true);
//        Role role = createMockRole(TEST_ROLE_NAME, "other_perm_id"); // Role has a different permission
//        Permission permission = createMockPermission(TEST_PERMISSION_NAME, TEST_PERMISSION_ID, true, null);
//
//        when(mongoTemplate.findById(eq(TEST_USER_ID), eq(User.class))).thenReturn(user);
//        when(mongoTemplate.find(any(Query.class), eq(Role.class))).thenReturn(Collections.singletonList(role));
//        when(mongoTemplate.findOne(any(Query.class), eq(Permission.class))).thenReturn(permission);
//
//        // Act
//        PermissionResult result = evaluationService.hasPermission(TEST_USER_ID, TEST_PERMISSION_NAME, "READ", null, Collections.emptyMap());
//
//        // Assert
//        assertFalse(result.isAllowed());
//        assertEquals("Permission not granted through roles", result.getReason());
//    }
//
//    // --- Time-Based Access Tests ---
//
//    // Helper for TimeAccessConfig
//    private TimeAccessConfig createTimeAccessConfig(boolean enabled, List<String> allowedDays, String startTime, String endTime) {
//        TimeAccessConfig tac = new TimeAccessConfig();
//        tac.setEnabled(enabled);
//        tac.setAllowedDays(allowedDays);
//        if (startTime != null && endTime != null) {
//            tac.setAllowedTimeWindows(Collections.singletonList(new TimeWindow(startTime, endTime)));
//        } else {
//            tac.setAllowedTimeWindows(Collections.emptyList());
//        }
//        // tac.setTimezone("Asia/Kathmandu"); // Timezone in JSON, not directly on this object for service
//        return tac;
//    }
//
//    @Test
//    void hasPermission_WithinAllowedTime_ShouldAllow() {
//        // Arrange
//        User user = createMockUser(TEST_USER_ID, TEST_ROLE_NAME, TEST_BRANCH_ID, true);
//        Role role = createMockRole(TEST_ROLE_NAME, TEST_PERMISSION_ID);
//
//        PermissionConfig permConfig = new PermissionConfig();
//        // Monday at 10:00 NPT
//        TimeAccessConfig timeConfig = createTimeAccessConfig(true, Collections.singletonList("MONDAY"), "09:00", "17:00");
//        permConfig.setTimeAccess(timeConfig);
//        Permission permission = createMockPermission(TEST_PERMISSION_NAME, TEST_PERMISSION_ID, true, permConfig);
//
//        when(mongoTemplate.findById(eq(TEST_USER_ID), eq(User.class))).thenReturn(user);
//        when(mongoTemplate.find(any(Query.class), eq(Role.class))).thenReturn(Collections.singletonList(role));
//        when(mongoTemplate.findOne(any(Query.class), eq(Permission.class))).thenReturn(permission);
//
//        // Mock LocalDateTime.now(NPT)
//        LocalDateTime fixedDateTime = LocalDateTime.of(2024, 3, 11, 10, 0, 0); // A Monday
//        try (MockedStatic<LocalDateTime> mockedStatic = Mockito.mockStatic(LocalDateTime.class, Mockito.CALLS_REAL_METHODS)) {
//            mockedStatic.when(() -> LocalDateTime.now(eq(NPT))).thenReturn(fixedDateTime);
//            mockedStatic.when(() -> LocalDateTime.now()).thenReturn(fixedDateTime); // if service uses now() without ZoneId
//
//            // Act
//            PermissionResult result = evaluationService.hasPermission(TEST_USER_ID, TEST_PERMISSION_NAME, "READ", null, Collections.emptyMap());
//
//            // Assert
//            assertTrue(result.isAllowed(), "Permission should be allowed within time: " + result.getReason());
//        }
//    }
//
//    @Test
//    void hasPermission_OutsideAllowedTime_ShouldDeny() {
//        // Arrange
//        User user = createMockUser(TEST_USER_ID, TEST_ROLE_NAME, TEST_BRANCH_ID, true);
//        Role role = createMockRole(TEST_ROLE_NAME, TEST_PERMISSION_ID);
//        PermissionConfig permConfig = new PermissionConfig();
//        // Monday at 18:00 NPT (outside 09:00-17:00 window)
//        TimeAccessConfig timeConfig = createTimeAccessConfig(true, Collections.singletonList("MONDAY"), "09:00", "17:00");
//        permConfig.setTimeAccess(timeConfig);
//        Permission permission = createMockPermission(TEST_PERMISSION_NAME, TEST_PERMISSION_ID, true, permConfig);
//
//        when(mongoTemplate.findById(eq(TEST_USER_ID), eq(User.class))).thenReturn(user);
//        when(mongoTemplate.find(any(Query.class), eq(Role.class))).thenReturn(Collections.singletonList(role));
//        when(mongoTemplate.findOne(any(Query.class), eq(Permission.class))).thenReturn(permission);
//
//        LocalDateTime fixedDateTime = LocalDateTime.of(2024, 3, 11, 18, 0, 0); // A Monday, but 6 PM
//         try (MockedStatic<LocalDateTime> mockedStatic = Mockito.mockStatic(LocalDateTime.class, Mockito.CALLS_REAL_METHODS)) {
//            mockedStatic.when(() -> LocalDateTime.now(eq(NPT))).thenReturn(fixedDateTime);
//            mockedStatic.when(() -> LocalDateTime.now()).thenReturn(fixedDateTime);
//
//            // Act
//            PermissionResult result = evaluationService.hasPermission(TEST_USER_ID, TEST_PERMISSION_NAME, "READ", null, Collections.emptyMap());
//
//            // Assert
//            assertFalse(result.isAllowed());
//            assertEquals("Access not allowed at current time", result.getReason());
//        }
//    }
//
//    @Test
//    void hasPermission_OutsideAllowedDay_ShouldDeny() {
//        // Arrange
//        User user = createMockUser(TEST_USER_ID, TEST_ROLE_NAME, TEST_BRANCH_ID, true);
//        Role role = createMockRole(TEST_ROLE_NAME, TEST_PERMISSION_ID);
//        PermissionConfig permConfig = new PermissionConfig();
//        // Tuesday at 10:00 NPT, but only Monday allowed
//        TimeAccessConfig timeConfig = createTimeAccessConfig(true, Collections.singletonList("MONDAY"), "09:00", "17:00");
//        permConfig.setTimeAccess(timeConfig);
//        Permission permission = createMockPermission(TEST_PERMISSION_NAME, TEST_PERMISSION_ID, true, permConfig);
//
//        when(mongoTemplate.findById(eq(TEST_USER_ID), eq(User.class))).thenReturn(user);
//        when(mongoTemplate.find(any(Query.class), eq(Role.class))).thenReturn(Collections.singletonList(role));
//        when(mongoTemplate.findOne(any(Query.class), eq(Permission.class))).thenReturn(permission);
//
//        LocalDateTime fixedDateTime = LocalDateTime.of(2024, 3, 12, 10, 0, 0); // A Tuesday
//        try (MockedStatic<LocalDateTime> mockedStatic = Mockito.mockStatic(LocalDateTime.class, Mockito.CALLS_REAL_METHODS)) {
//            mockedStatic.when(() -> LocalDateTime.now(eq(NPT))).thenReturn(fixedDateTime);
//            mockedStatic.when(() -> LocalDateTime.now()).thenReturn(fixedDateTime);
//
//            // Act
//            PermissionResult result = evaluationService.hasPermission(TEST_USER_ID, TEST_PERMISSION_NAME, "READ", null, Collections.emptyMap());
//
//            // Assert
//            assertFalse(result.isAllowed());
//            assertEquals("Access not allowed on TUESDAY", result.getReason());
//        }
//    }
//
//    // --- Amount Limit Tests ---
//    private AmountLimitConfig createAmountLimitConfig(boolean enabled, String limitType, Double defaultLimit, String amountFieldPath, Map<String, Double> roleLimits) {
//        AmountLimitConfig alc = new AmountLimitConfig();
//        alc.setEnabled(enabled);
//        alc.setLimitType(limitType);
//        alc.setDefaultLimit(defaultLimit);
//        alc.setAmountFieldPath(amountFieldPath);
//        alc.setRoleLimits(roleLimits);
//        return alc;
//    }
//
//    @Test
//    void hasPermission_AmountWithinFixedLimit_ShouldAllow() {
//        // Arrange
//        User user = createMockUser(TEST_USER_ID, TEST_ROLE_NAME, TEST_BRANCH_ID, true);
//        Role role = createMockRole(TEST_ROLE_NAME, TEST_PERMISSION_ID);
//        PermissionConfig permConfig = new PermissionConfig();
//        AmountLimitConfig amountConfig = createAmountLimitConfig(true, "FIXED", 1000.0, "amount", null);
//        permConfig.setAmountLimit(amountConfig);
//        Permission permission = createMockPermission(TEST_PERMISSION_NAME, TEST_PERMISSION_ID, true, permConfig);
//
//        Map<String, Object> resourceContext = new HashMap<>(); // Using context map for amount
//        resourceContext.put("amount", 500.0); // Amount to check
//
//        when(mongoTemplate.findById(eq(TEST_USER_ID), eq(User.class))).thenReturn(user);
//        when(mongoTemplate.find(any(Query.class), eq(Role.class))).thenReturn(Collections.singletonList(role));
//        when(mongoTemplate.findOne(any(Query.class), eq(Permission.class))).thenReturn(permission);
//        // Simulate that extractAmount will get it from context
//        // The service's extractAmount method needs to be able to pull from context or resource object.
//        // For this test, we assume extractAmount is called with context.
//
//        // Act
//        // Pass null for resource, but context has amount
//        PermissionResult result = evaluationService.hasPermission(TEST_USER_ID, TEST_PERMISSION_NAME, "CREATE", null, resourceContext);
//
//        // Assert
//        assertTrue(result.isAllowed(), "Permission should be allowed: " + result.getReason());
//    }
//
//    @Test
//    void hasPermission_AmountExceedsFixedLimit_ShouldDeny() {
//        // Arrange
//        User user = createMockUser(TEST_USER_ID, TEST_ROLE_NAME, TEST_BRANCH_ID, true);
//        Role role = createMockRole(TEST_ROLE_NAME, TEST_PERMISSION_ID);
//        PermissionConfig permConfig = new PermissionConfig();
//        AmountLimitConfig amountConfig = createAmountLimitConfig(true, "FIXED", 1000.0, "amount", null);
//        permConfig.setAmountLimit(amountConfig);
//        Permission permission = createMockPermission(TEST_PERMISSION_NAME, TEST_PERMISSION_ID, true, permConfig);
//
//        Map<String, Object> resourceContext = new HashMap<>();
//        resourceContext.put("amount", 1500.0);
//
//        when(mongoTemplate.findById(eq(TEST_USER_ID), eq(User.class))).thenReturn(user);
//        when(mongoTemplate.find(any(Query.class), eq(Role.class))).thenReturn(Collections.singletonList(role));
//        when(mongoTemplate.findOne(any(Query.class), eq(Permission.class))).thenReturn(permission);
//
//        // Act
//        PermissionResult result = evaluationService.hasPermission(TEST_USER_ID, TEST_PERMISSION_NAME, "CREATE", null, resourceContext);
//
//        // Assert
//        assertFalse(result.isAllowed());
//        assertEquals(String.format("Amount %.2f exceeds limit %.2f", 1500.0, 1000.0), result.getReason());
//    }
//
//    @Test
//    void hasPermission_AmountWithinRoleBasedLimit_ShouldAllow() {
//        User user = createMockUser(TEST_USER_ID, TEST_ROLE_NAME, TEST_BRANCH_ID, true);
//        Role role = createMockRole(TEST_ROLE_NAME, TEST_PERMISSION_ID); // Role name is TEST_ROLE_NAME
//
//        PermissionConfig permConfig = new PermissionConfig();
//        Map<String, Double> roleLimits = new HashMap<>();
//        roleLimits.put(TEST_ROLE_NAME, 2000.0); // User's role has limit 2000
//        roleLimits.put("OTHER_ROLE", 500.0);
//        AmountLimitConfig amountConfig = createAmountLimitConfig(true, "ROLE_BASED", 100.0, "amount", roleLimits);
//        permConfig.setAmountLimit(amountConfig);
//        Permission permission = createMockPermission(TEST_PERMISSION_NAME, TEST_PERMISSION_ID, true, permConfig);
//
//        Map<String, Object> resourceContext = new HashMap<>();
//        resourceContext.put("amount", 1800.0);
//
//        when(mongoTemplate.findById(eq(TEST_USER_ID), eq(User.class))).thenReturn(user);
//        // Important: mock the query for user's roles correctly to return the role object
//        when(mongoTemplate.find(Query.query(org.springframework.data.mongodb.core.query.Criteria.where("name").in(user.getRoleIds()).and("active").is(true)), eq(Role.class)))
//            .thenReturn(Collections.singletonList(role));
//        when(mongoTemplate.findOne(any(Query.class), eq(Permission.class))).thenReturn(permission);
//
//        // Act
//        PermissionResult result = evaluationService.hasPermission(TEST_USER_ID, TEST_PERMISSION_NAME, "CREATE", null, resourceContext);
//
//        // Assert
//        assertTrue(result.isAllowed(), "Permission should be allowed: " + result.getReason());
//    }
//
//    // --- Branch Access Tests ---
//    private BranchAccessConfig createBranchAccessConfig(String type, String branchFieldPath, List<String> allowedBranches, boolean includeSubBranches) {
//        BranchAccessConfig bac = new BranchAccessConfig();
//        bac.setType(type);
//        bac.setBranchFieldPath(branchFieldPath); // e.g., "branchId" if resource is a Map/POJO
//        bac.setAllowedBranches(allowedBranches);
//        bac.setIncludeSubBranches(includeSubBranches);
//        return bac;
//    }
//
//    // Mock resource for branch tests
//    private Map<String, String> createMockResourceWithBranch(String branchId) {
//        Map<String, String> resource = new HashMap<>();
//        resource.put("branchId", branchId); // Assuming branchFieldPath is "branchId"
//        return resource;
//    }
//
//
//    @Test
//    void hasPermission_OwnBranchAccess_UserInResourceBranch_ShouldAllow() {
//        User user = createMockUser(TEST_USER_ID, TEST_ROLE_NAME, TEST_BRANCH_ID, true); // User in TEST_BRANCH_ID
//        Role role = createMockRole(TEST_ROLE_NAME, TEST_PERMISSION_ID);
//        PermissionConfig permConfig = new PermissionConfig();
//        // IMPORTANT: The 'extractBranchId' helper in the service needs to correctly get this.
//        // If resource is a Map, field path "branchId" would mean map.get("branchId").
//        BranchAccessConfig branchConfig = createBranchAccessConfig("OWN_BRANCH", "branchId", null, false);
//        permConfig.setBranchAccess(branchConfig);
//        Permission permission = createMockPermission(TEST_PERMISSION_NAME, TEST_PERMISSION_ID, true, permConfig);
//
//        Map<String, String> resource = createMockResourceWithBranch(TEST_BRANCH_ID); // Resource also in TEST_BRANCH_ID
//
//        when(mongoTemplate.findById(eq(TEST_USER_ID), eq(User.class))).thenReturn(user);
//        when(mongoTemplate.find(any(Query.class), eq(Role.class))).thenReturn(Collections.singletonList(role));
//        when(mongoTemplate.findOne(any(Query.class), eq(Permission.class))).thenReturn(permission);
//
//        // Act
//        PermissionResult result = evaluationService.hasPermission(TEST_USER_ID, TEST_PERMISSION_NAME, "READ", resource, Collections.emptyMap());
//
//        // Assert
//        assertTrue(result.isAllowed(), "Permission should be allowed for own branch: " + result.getReason());
//    }
//
//    @Test
//    void hasPermission_OwnBranchAccess_UserNotInResourceBranch_ShouldDeny() {
//        User user = createMockUser(TEST_USER_ID, TEST_ROLE_NAME, TEST_BRANCH_ID, true); // User in TEST_BRANCH_ID
//        Role role = createMockRole(TEST_ROLE_NAME, TEST_PERMISSION_ID);
//        PermissionConfig permConfig = new PermissionConfig();
//        BranchAccessConfig branchConfig = createBranchAccessConfig("OWN_BRANCH", "branchId", null, false);
//        permConfig.setBranchAccess(branchConfig);
//        Permission permission = createMockPermission(TEST_PERMISSION_NAME, TEST_PERMISSION_ID, true, permConfig);
//
//        Map<String, String> resource = createMockResourceWithBranch(OTHER_BRANCH_ID); // Resource in OTHER_BRANCH_ID
//
//        when(mongoTemplate.findById(eq(TEST_USER_ID), eq(User.class))).thenReturn(user);
//        when(mongoTemplate.find(any(Query.class), eq(Role.class))).thenReturn(Collections.singletonList(role));
//        when(mongoTemplate.findOne(any(Query.class), eq(Permission.class))).thenReturn(permission);
//
//        // Act
//        PermissionResult result = evaluationService.hasPermission(TEST_USER_ID, TEST_PERMISSION_NAME, "READ", resource, Collections.emptyMap());
//
//        // Assert
//        assertFalse(result.isAllowed());
//        assertEquals("Access limited to own branch", result.getReason());
//    }
//
//    @Test
//    void hasPermission_AllBranchesAccess_ShouldAllow() {
//        User user = createMockUser(TEST_USER_ID, TEST_ROLE_NAME, OTHER_BRANCH_ID, true); // User in any branch
//        Role role = createMockRole(TEST_ROLE_NAME, TEST_PERMISSION_ID);
//        PermissionConfig permConfig = new PermissionConfig();
//        BranchAccessConfig branchConfig = createBranchAccessConfig("ALL_BRANCHES", null, null, false);
//        permConfig.setBranchAccess(branchConfig);
//        Permission permission = createMockPermission(TEST_PERMISSION_NAME, TEST_PERMISSION_ID, true, permConfig);
//
//        Map<String, String> resource = createMockResourceWithBranch(TEST_BRANCH_ID); // Resource in a specific branch
//
//        when(mongoTemplate.findById(eq(TEST_USER_ID), eq(User.class))).thenReturn(user);
//        when(mongoTemplate.find(any(Query.class), eq(Role.class))).thenReturn(Collections.singletonList(role));
//        when(mongoTemplate.findOne(any(Query.class), eq(Permission.class))).thenReturn(permission);
//
//        // Act
//        PermissionResult result = evaluationService.hasPermission(TEST_USER_ID, TEST_PERMISSION_NAME, "READ", resource, Collections.emptyMap());
//
//        // Assert
//        assertTrue(result.isAllowed(), "Permission should be allowed for all branches: " + result.getReason());
//    }
//
//    @Test
//    void hasPermission_BranchHierarchyAccess_AllowedByHierarchy_ShouldAllow() {
//        User user = createMockUser(TEST_USER_ID, TEST_ROLE_NAME, TEST_BRANCH_ID, true);
//        Role role = createMockRole(TEST_ROLE_NAME, TEST_PERMISSION_ID);
//        PermissionConfig permConfig = new PermissionConfig();
//        BranchAccessConfig branchConfig = createBranchAccessConfig("BRANCH_HIERARCHY", "branchId", null, true);
//        permConfig.setBranchAccess(branchConfig);
//        Permission permission = createMockPermission(TEST_PERMISSION_NAME, TEST_PERMISSION_ID, true, permConfig);
//
//        Map<String, String> resource = createMockResourceWithBranch(OTHER_BRANCH_ID);
//
//        when(mongoTemplate.findById(eq(TEST_USER_ID), eq(User.class))).thenReturn(user);
//        when(mongoTemplate.find(any(Query.class), eq(Role.class))).thenReturn(Collections.singletonList(role));
//        when(mongoTemplate.findOne(any(Query.class), eq(Permission.class))).thenReturn(permission);
//        when(branchHierarchyService.hasAccessToBranch(eq(TEST_BRANCH_ID), eq(OTHER_BRANCH_ID), eq(true))).thenReturn(true);
//
//        // Act
//        PermissionResult result = evaluationService.hasPermission(TEST_USER_ID, TEST_PERMISSION_NAME, "READ", resource, Collections.emptyMap());
//
//        // Assert
//        assertTrue(result.isAllowed(), "Permission should be allowed by branch hierarchy: " + result.getReason());
//    }
//
//
//    // --- SpEL Condition Tests ---
//    @Test
//    void hasPermission_SpELConditionTrue_ShouldAllow() {
//        User user = createMockUser(TEST_USER_ID, TEST_ROLE_NAME, TEST_BRANCH_ID, true);
//        Role role = createMockRole(TEST_ROLE_NAME, TEST_PERMISSION_ID);
//        PermissionConfig permConfig = new PermissionConfig();
//        permConfig.setConditions(Collections.singletonList("#user.branchId == '" + TEST_BRANCH_ID + "'")); // Simple SpEL
//        Permission permission = createMockPermission(TEST_PERMISSION_NAME, TEST_PERMISSION_ID, true, permConfig);
//
//        // Mock Branch for SpEL context if getBranch is called
//        Branch mockBranch = new Branch();
//        mockBranch.setId(TEST_BRANCH_ID);
//        mockBranch.setName("Test Branch");
//        lenient().when(mongoTemplate.findById(eq(TEST_BRANCH_ID), eq(Branch.class))).thenReturn(mockBranch);
//
//
//        when(mongoTemplate.findById(eq(TEST_USER_ID), eq(User.class))).thenReturn(user);
//        when(mongoTemplate.find(any(Query.class), eq(Role.class))).thenReturn(Collections.singletonList(role));
//        when(mongoTemplate.findOne(any(Query.class), eq(Permission.class))).thenReturn(permission);
//
//
//        LocalDateTime fixedDateTime = LocalDateTime.of(2024, 3, 11, 10, 0, 0); // To make SpEL #now consistent
//        try (MockedStatic<LocalDateTime> mockedStatic = Mockito.mockStatic(LocalDateTime.class, Mockito.CALLS_REAL_METHODS)) {
//            mockedStatic.when(() -> LocalDateTime.now(eq(NPT))).thenReturn(fixedDateTime);
//            mockedStatic.when(() -> LocalDateTime.now()).thenReturn(fixedDateTime);
//
//            // Act
//            PermissionResult result = evaluationService.hasPermission(TEST_USER_ID, TEST_PERMISSION_NAME, "READ", null, Collections.emptyMap());
//
//            // Assert
//            assertTrue(result.isAllowed(), "Permission should be allowed by SpEL: " + result.getReason());
//        }
//    }
//
//    @Test
//    void hasPermission_SpELConditionFalse_ShouldDeny() {
//        User user = createMockUser(TEST_USER_ID, TEST_ROLE_NAME, TEST_BRANCH_ID, true);
//        Role role = createMockRole(TEST_ROLE_NAME, TEST_PERMISSION_ID);
//        PermissionConfig permConfig = new PermissionConfig();
//        permConfig.setConditions(Collections.singletonList("#user.branchId == 'someOtherBranchId'")); // False condition
//        Permission permission = createMockPermission(TEST_PERMISSION_NAME, TEST_PERMISSION_ID, true, permConfig);
//
//        lenient().when(mongoTemplate.findById(eq(TEST_BRANCH_ID), eq(Branch.class))).thenReturn(new Branch()); // For #userBranch
//
//        when(mongoTemplate.findById(eq(TEST_USER_ID), eq(User.class))).thenReturn(user);
//        when(mongoTemplate.find(any(Query.class), eq(Role.class))).thenReturn(Collections.singletonList(role));
//        when(mongoTemplate.findOne(any(Query.class), eq(Permission.class))).thenReturn(permission);
//
//        LocalDateTime fixedDateTime = LocalDateTime.of(2024, 3, 11, 10, 0, 0); // To make SpEL #now consistent
//        try (MockedStatic<LocalDateTime> mockedStatic = Mockito.mockStatic(LocalDateTime.class, Mockito.CALLS_REAL_METHODS)) {
//            mockedStatic.when(() -> LocalDateTime.now(eq(NPT))).thenReturn(fixedDateTime);
//             mockedStatic.when(() -> LocalDateTime.now()).thenReturn(fixedDateTime);
//
//            // Act
//            PermissionResult result = evaluationService.hasPermission(TEST_USER_ID, TEST_PERMISSION_NAME, "READ", null, Collections.emptyMap());
//
//            // Assert
//            assertFalse(result.isAllowed());
//            assertEquals("Custom condition failed: #user.branchId == 'someOtherBranchId'", result.getReason());
//        }
//    }
//
//    // --- User Overrides (Temporary Permission) ---
//    @Test
//    void hasPermission_ActiveTemporaryPermissionGrantsAccess_ShouldAllow() {
//        User user = createMockUser(TEST_USER_ID, TEST_ROLE_NAME, TEST_BRANCH_ID, true);
//        // User does NOT have the role for TEST_PERMISSION_ID directly
//        Role role = createMockRole(TEST_ROLE_NAME, "unrelated_perm_id");
//
//        TemporaryPermission tempPerm = new TemporaryPermission();
//        tempPerm.setPermissionId(TEST_PERMISSION_NAME); // Matches by name
//        tempPerm.setExpiresAt(LocalDateTime.now(NPT).plusDays(1)); // Active
//        tempPerm.setConditions(Collections.emptyList()); // No extra conditions on temp perm
//
//        UserPermissionConfig userPermConfig = new UserPermissionConfig();
//        userPermConfig.setTemporaryPermissions(Collections.singletonList(tempPerm));
//        user.setPermissionConfig(userPermConfig);
//
//        Permission permissionDefinition = createMockPermission(TEST_PERMISSION_NAME, TEST_PERMISSION_ID, true, null); // Base permission def
//
//        when(mongoTemplate.findById(eq(TEST_USER_ID), eq(User.class))).thenReturn(user);
//        // Role query will return the role, but it won't grant the permission directly
//        when(mongoTemplate.find(any(Query.class), eq(Role.class))).thenReturn(Collections.singletonList(role));
//        when(mongoTemplate.findOne(any(Query.class), eq(Permission.class))).thenReturn(permissionDefinition);
//
//        LocalDateTime fixedDateTime = LocalDateTime.now(NPT); // Use a fixed "now" for temp perm check
//        try (MockedStatic<LocalDateTime> mockedStatic = Mockito.mockStatic(LocalDateTime.class, Mockito.CALLS_REAL_METHODS)) {
//            mockedStatic.when(() -> LocalDateTime.now(eq(NPT))).thenReturn(fixedDateTime);
//            mockedStatic.when(() -> LocalDateTime.now()).thenReturn(fixedDateTime);
//
//            // Act
//            PermissionResult result = evaluationService.hasPermission(TEST_USER_ID, TEST_PERMISSION_NAME, "READ", null, Collections.emptyMap());
//
//            // Assert
//            assertTrue(result.isAllowed(), "Permission should be allowed by active temporary permission: " + result.getReason());
//            assertEquals("Temporary permission granted", result.getReason());
//        }
//    }
//
//    @Test
//    void hasPermission_ExpiredTemporaryPermission_ShouldDenyIfNoOtherAccess() {
//        User user = createMockUser(TEST_USER_ID, TEST_ROLE_NAME, TEST_BRANCH_ID, true);
//        Role role = createMockRole(TEST_ROLE_NAME, "unrelated_perm_id");
//
//        TemporaryPermission tempPerm = new TemporaryPermission();
//        tempPerm.setPermissionId(TEST_PERMISSION_NAME);
//        tempPerm.setExpiresAt(LocalDateTime.now(NPT).minusDays(1)); // Expired
//        UserPermissionConfig userPermConfig = new UserPermissionConfig();
//        userPermConfig.setTemporaryPermissions(Collections.singletonList(tempPerm));
//        user.setPermissionConfig(userPermConfig);
//
//        Permission permissionDefinition = createMockPermission(TEST_PERMISSION_NAME, TEST_PERMISSION_ID, true, null);
//
//        when(mongoTemplate.findById(eq(TEST_USER_ID), eq(User.class))).thenReturn(user);
//        when(mongoTemplate.find(any(Query.class), eq(Role.class))).thenReturn(Collections.singletonList(role));
//        when(mongoTemplate.findOne(any(Query.class), eq(Permission.class))).thenReturn(permissionDefinition);
//
//        LocalDateTime fixedDateTime = LocalDateTime.now(NPT);
//        try (MockedStatic<LocalDateTime> mockedStatic = Mockito.mockStatic(LocalDateTime.class, Mockito.CALLS_REAL_METHODS)) {
//            mockedStatic.when(() -> LocalDateTime.now(eq(NPT))).thenReturn(fixedDateTime);
//            mockedStatic.when(() -> LocalDateTime.now()).thenReturn(fixedDateTime);
//
//            // Act
//            PermissionResult result = evaluationService.hasPermission(TEST_USER_ID, TEST_PERMISSION_NAME, "READ", null, Collections.emptyMap());
//
//            // Assert
//            assertFalse(result.isAllowed(), "Permission should be denied as temporary permission is expired and no other access path.");
//            assertEquals("Permission not granted through roles", result.getReason()); // Falls through to role check
//        }
//    }
//}
