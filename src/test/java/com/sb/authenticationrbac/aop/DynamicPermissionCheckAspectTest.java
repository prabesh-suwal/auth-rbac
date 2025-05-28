//package com.sb.authenticationrbac.aop;
//
//import com.sb.authenticationrbac.dtos.PermissionResult;
//import com.sb.authenticationrbac.exceptions.PermissionDeniedException;
//import com.sb.authenticationrbac.repositories.UserRepository;
//import com.sb.authenticationrbac.security.UserPrincipal;
//import com.sb.authenticationrbac.services.DynamicPermissionEvaluationService;
//import jakarta.servlet.http.HttpServletRequest;
//import org.aspectj.lang.ProceedingJoinPoint;
//import org.aspectj.lang.reflect.MethodSignature;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.context.SecurityContext;
//import org.springframework.security.core.context.SecurityContextHolder;
//
//import java.lang.reflect.Method;
//import java.lang.reflect.Parameter;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.Map;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//import static org.mockito.Mockito.withSettings;
//
//@ExtendWith(MockitoExtension.class)
//class DynamicPermissionCheckAspectTest {
//
//    @Mock
//    private UserRepository userRepository;
//
//    @Mock
//    private DynamicPermissionEvaluationService permissionService;
//
//    @Mock
//    private ResourceLoaderService resourceLoaderService;
//
//    @Mock
//    private HttpServletRequest request;
//
//    @Mock
//    private ProceedingJoinPoint joinPoint;
//
//    @Mock
//    private MethodSignature methodSignature;
//
//    @Mock
//    private Method method;
//
//    @Mock
//    private SecurityContext securityContext;
//
//    @Mock
//    private Authentication authentication;
//
//    private DynamicPermissionCheckAspect aspect;
//    private UserPrincipal mockUserPrincipal;
//
//    @BeforeEach
//    void setUp() {
//        // Initialize the aspect with mocked dependencies
//        aspect = new DynamicPermissionCheckAspect(userRepository, permissionService, resourceLoaderService, request);
//
//        // Create a mock UserPrincipal
//        mockUserPrincipal = new UserPrincipal(
//            "testuser123",
//            "testuser",
//            "test@example.com",
//            "password",
//            "branch1",
//            true,
//            Arrays.asList(
//                new SimpleGrantedAuthority("USER_CREATE"),
//                new SimpleGrantedAuthority("USER_VIEW"),
//                new SimpleGrantedAuthority("ROLE_USER")
//            )
//        );
//
//        // Setup security context
//        SecurityContextHolder.setContext(securityContext);
//        when(securityContext.getAuthentication()).thenReturn(authentication);
//        when(authentication.getPrincipal()).thenReturn(mockUserPrincipal);
//        when(authentication.isAuthenticated()).thenReturn(true);
//        when(authentication.getName()).thenReturn("testuser");
//    }
//
//    @Test
//    void checkSinglePermission_WhenPermissionDenied_ShouldThrowExceptionWithDetails() throws Throwable {
//        // Arrange
//        String permissionName = "USER_CREATE";
//        String operation = "CREATE";
//        String customMessage = "Custom access denied message";
//
//        CheckPermission checkPermission = createMockCheckPermission(permissionName, operation, customMessage);
//
//        // Mock the permission evaluation to return denied
//        PermissionResult deniedResult = PermissionResult.denied("Permission not granted through roles");
//        when(permissionService.hasPermission(eq("testuser123"), eq(permissionName), eq(operation), any(), any()))
//                .thenReturn(deniedResult);
//
//        // Mock method signature and parameters
//        setupMockMethodSignature();
//
//        // Mock request details
//        when(request.getMethod()).thenReturn("POST");
//        when(request.getRequestURI()).thenReturn("/api/v1/users");
//        when(request.getParameterMap()).thenReturn(new HashMap<>());
//
//        // Mock resource loading
//        Map<String, Object> mockResource = new HashMap<>();
//        mockResource.put("id", "user123");
//        mockResource.put("name", "Test User");
//        when(resourceLoaderService.loadResource(anyString(), any())).thenReturn(mockResource);
//
//        // Act & Assert
//        PermissionDeniedException exception = assertThrows(PermissionDeniedException.class, () -> {
//            aspect.checkSinglePermission(joinPoint, checkPermission);
//        });
//
//        // Verify exception contains the permission details - expect the actual message from PermissionResult
//        assertEquals("Permission not granted through roles", exception.getMessage());
//        assertEquals(permissionName, exception.getPermissionName());
//        assertEquals(operation, exception.getOperation());
//        assertNotNull(exception.getResource());
//        assertTrue(exception.getResource().contains("user123")); // Resource should be toString() of the mock resource
//        assertNotNull(exception.getContext());
//
//        // Verify context contains expected information
//        Map<String, Object> context = exception.getContext();
//        assertEquals("POST", context.get("requestMethod"));
//        assertEquals("/api/v1/users", context.get("requestPath"));
//        assertEquals("testuser", context.get("authenticationName"));
//
//        System.out.println("Exception details:");
//        System.out.println("Message: " + exception.getMessage());
//        System.out.println("Permission: " + exception.getPermissionName());
//        System.out.println("Resource: " + exception.getResource());
//        System.out.println("Operation: " + exception.getOperation());
//        System.out.println("Context keys: " + context.keySet());
//    }
//
//    @Test
//    void checkSinglePermission_WhenPermissionDeniedWithDefaultMessage_ShouldUseResultReason() throws Throwable {
//        // Arrange
//        String permissionName = "USER_VIEW";
//        String operation = "READ";
//        String resultReason = "User not found or inactive";
//
//        CheckPermission checkPermission = createMockCheckPermission(permissionName, operation, ""); // Empty message
//
//        PermissionResult deniedResult = PermissionResult.denied(resultReason);
//        when(permissionService.hasPermission(eq("testuser123"), eq(permissionName), eq(operation), any(), any()))
//                .thenReturn(deniedResult);
//
//        setupMockMethodSignature();
//        when(request.getMethod()).thenReturn("GET");
//        when(request.getRequestURI()).thenReturn("/api/v1/users/123");
//        when(request.getParameterMap()).thenReturn(new HashMap<>());
//
//        // Act & Assert
//        PermissionDeniedException exception = assertThrows(PermissionDeniedException.class, () -> {
//            aspect.checkSinglePermission(joinPoint, checkPermission);
//        });
//
//        // Should use the result reason as message when custom message is empty
//        assertEquals(resultReason, exception.getMessage());
//        assertEquals(permissionName, exception.getPermissionName());
//        assertEquals(operation, exception.getOperation());
//    }
//
//    @Test
//    void checkMultiplePermissions_WhenPermissionDenied_ShouldThrowExceptionWithDetails() throws Throwable {
//        // Arrange
//        CheckPermission permission1 = createMockCheckPermission("USER_CREATE", "CREATE", "");
//        CheckPermission permission2 = createMockCheckPermission("USER_UPDATE", "UPDATE", "");
//        CheckPermissions checkPermissions = createMockCheckPermissions(new CheckPermission[]{permission1, permission2}, "AND", "Multiple permissions denied");
//
//        // First permission succeeds, second fails
//        when(permissionService.hasPermission(eq("testuser123"), eq("USER_CREATE"), eq("CREATE"), any(), any()))
//                .thenReturn(PermissionResult.allowed("Permission granted"));
//        when(permissionService.hasPermission(eq("testuser123"), eq("USER_UPDATE"), eq("UPDATE"), any(), any()))
//                .thenReturn(PermissionResult.denied("Update permission denied"));
//
//        setupMockMethodSignature();
//        when(request.getMethod()).thenReturn("PUT");
//        when(request.getRequestURI()).thenReturn("/api/v1/users/123");
//        when(request.getParameterMap()).thenReturn(new HashMap<>());
//
//        // Act & Assert
//        PermissionDeniedException exception = assertThrows(PermissionDeniedException.class, () -> {
//            aspect.checkMultiplePermissions(joinPoint, checkPermissions);
//        });
//
//        // Should contain details of the failing permission
//        assertEquals("Multiple permissions denied", exception.getMessage());
//        assertEquals("USER_UPDATE", exception.getPermissionName());
//        assertEquals("UPDATE", exception.getOperation());
//        assertNotNull(exception.getContext());
//    }
//
//    @Test
//    void checkSinglePermission_WhenPermissionAllowed_ShouldProceed() throws Throwable {
//        // Arrange
//        String permissionName = "USER_VIEW";
//        String operation = "READ";
//
//        CheckPermission checkPermission = createMockCheckPermission(permissionName, operation, "");
//
//        // Mock the permission evaluation to return allowed
//        PermissionResult allowedResult = PermissionResult.allowed("Permission granted");
//        when(permissionService.hasPermission(eq("testuser123"), eq(permissionName), eq(operation), any(), any()))
//                .thenReturn(allowedResult);
//
//        setupMockMethodSignature();
//        when(request.getMethod()).thenReturn("GET");
//        when(request.getRequestURI()).thenReturn("/api/v1/users/123");
//        when(request.getParameterMap()).thenReturn(new HashMap<>());
//
//        // Mock joinPoint.proceed()
//        Object expectedResult = "success";
//        when(joinPoint.proceed()).thenReturn(expectedResult);
//
//        // Act
//        Object result = aspect.checkSinglePermission(joinPoint, checkPermission);
//
//        // Assert
//        assertEquals(expectedResult, result);
//        verify(joinPoint).proceed();
//    }
//
//    private CheckPermission createMockCheckPermission(String value, String operation, String message) {
//        CheckPermission checkPermission = mock(CheckPermission.class, withSettings().lenient());
//        when(checkPermission.value()).thenReturn(value);
//        when(checkPermission.operation()).thenReturn(operation);
//        when(checkPermission.message()).thenReturn(message);
//        when(checkPermission.resource()).thenReturn("USER");
//        when(checkPermission.resourceIdParam()).thenReturn("id");
//        when(checkPermission.resourceParam()).thenReturn("");
//        when(checkPermission.contextParams()).thenReturn(new String[]{});
//        return checkPermission;
//    }
//
//    private CheckPermissions createMockCheckPermissions(CheckPermission[] permissions, String logic, String message) {
//        CheckPermissions checkPermissions = mock(CheckPermissions.class, withSettings().lenient());
//        when(checkPermissions.value()).thenReturn(permissions);
//        when(checkPermissions.logic()).thenReturn(logic);
//        when(checkPermissions.message()).thenReturn(message);
//        return checkPermissions;
//    }
//
//    private void setupMockMethodSignature() {
//        when(joinPoint.getSignature()).thenReturn(methodSignature);
//        when(methodSignature.getMethod()).thenReturn(method);
//        when(methodSignature.getDeclaringTypeName()).thenReturn("com.sb.authenticationrbac.controllers.UserController");
//        when(method.getName()).thenReturn("createUser");
//
//        // Mock method parameters
//        Parameter[] parameters = new Parameter[0]; // No parameters for simplicity
//        Object[] args = new Object[0];
//        when(method.getParameters()).thenReturn(parameters);
//        when(joinPoint.getArgs()).thenReturn(args);
//    }
//}