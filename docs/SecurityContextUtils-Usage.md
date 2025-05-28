# SecurityContextUtils Usage Guide

The `SecurityContextUtils` class provides a convenient way to access the current authenticated user's information from anywhere in your Spring Boot application. This utility class extracts user details from the Spring Security context.

## Overview

The `SecurityContextUtils` is a static utility class that provides easy access to:
- Current user's `UserPrincipal`
- User ID, username, email, and branch ID
- User authorities and permissions
- Role-based checks
- Authentication status

## Basic Usage

### Getting Current User Information

```java
import com.sb.authenticationrbac.security.SecurityContextUtils;

// Get current user principal (optional)
Optional<UserPrincipal> userPrincipal = SecurityContextUtils.getCurrentUserPrincipal();

// Get current user ID (optional)
Optional<String> userId = SecurityContextUtils.getCurrentUserId();

// Get current username (optional)
Optional<String> username = SecurityContextUtils.getCurrentUsername();

// Get current user email (optional)
Optional<String> email = SecurityContextUtils.getCurrentUserEmail();

// Get current user's branch ID (optional)
Optional<String> branchId = SecurityContextUtils.getCurrentUserBranchId();
```

### Required Methods (Throw Exception if Not Authenticated)

```java
// Get current user ID (throws exception if not authenticated)
String userId = SecurityContextUtils.requireCurrentUserId();

// Get current user principal (throws exception if not authenticated)
UserPrincipal user = SecurityContextUtils.requireCurrentUserPrincipal();
```

### Authentication Status

```java
// Check if user is authenticated
boolean isAuthenticated = SecurityContextUtils.isAuthenticated();

// Get current authentication object
Optional<Authentication> auth = SecurityContextUtils.getCurrentAuthentication();
```

## Permission and Role Checking

### Authority/Permission Checks

```java
// Check if user has a specific permission
boolean hasUserCreate = SecurityContextUtils.hasAuthority("USER_CREATE");
boolean hasLoanApprove = SecurityContextUtils.hasAuthority("LOAN_APPROVE");

// Check if user has any of the specified permissions
boolean hasAnyUserPermission = SecurityContextUtils.hasAnyAuthority(
    "USER_CREATE", "USER_UPDATE", "USER_DELETE"
);
```

### Role Checks

```java
// Check if user has a specific role (without ROLE_ prefix)
boolean isAdmin = SecurityContextUtils.hasRole("ADMIN");
boolean isManager = SecurityContextUtils.hasRole("MANAGER");

// Check if user has any of the specified roles
boolean isAdminOrManager = SecurityContextUtils.hasAnyRole("ADMIN", "MANAGER");
```

## Usage in Services

### Example Service Implementation

```java
@Service
public class LoanService {
    
    public void approveLoan(String loanId) {
        // Get current user for audit logging
        String currentUserId = SecurityContextUtils.requireCurrentUserId();
        String auditInfo = SecurityContextUtils.getCurrentUserPrincipal()
            .map(user -> String.format("User[%s] from branch[%s]", 
                user.getUsername(), user.getBranchId()))
            .orElse("Unknown user");
        
        // Check permissions
        if (!SecurityContextUtils.hasAuthority("LOAN_APPROVE")) {
            throw new AccessDeniedException("User does not have loan approval permission");
        }
        
        // Business logic here
        log.info("Loan {} approved by {}", loanId, auditInfo);
    }
    
    public List<Loan> getUserLoans() {
        // Get current user's branch for filtering
        String userBranchId = SecurityContextUtils.getCurrentUserBranchId()
            .orElseThrow(() -> new IllegalStateException("User branch not found"));
        
        // Admin can see all loans, others only their branch
        if (SecurityContextUtils.hasRole("ADMIN")) {
            return loanRepository.findAll();
        } else {
            return loanRepository.findByBranchId(userBranchId);
        }
    }
}
```

### Branch-Based Access Control

```java
@Service
public class BranchAccessService {
    
    public boolean canAccessBranch(String branchId) {
        // Admin can access all branches
        if (SecurityContextUtils.hasRole("ADMIN")) {
            return true;
        }
        
        // Users can access their own branch
        return SecurityContextUtils.getCurrentUserBranchId()
            .map(userBranchId -> userBranchId.equals(branchId))
            .orElse(false);
    }
    
    public List<String> getAccessibleBranches() {
        if (SecurityContextUtils.hasRole("ADMIN")) {
            return branchRepository.findAll().stream()
                .map(Branch::getId)
                .collect(Collectors.toList());
        }
        
        return SecurityContextUtils.getCurrentUserBranchId()
            .map(List::of)
            .orElse(Collections.emptyList());
    }
}
```

## Usage in Controllers

### Basic Controller Usage

```java
@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        return SecurityContextUtils.getCurrentUserPrincipal()
            .map(user -> ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "branchId", user.getBranchId()
            )))
            .orElse(ResponseEntity.badRequest()
                .body(new MessageResponse("No authenticated user")));
    }
    
    @GetMapping("/permissions")
    public ResponseEntity<?> getUserPermissions() {
        if (!SecurityContextUtils.isAuthenticated()) {
            return ResponseEntity.badRequest()
                .body(new MessageResponse("Authentication required"));
        }
        
        UserPrincipal user = SecurityContextUtils.requireCurrentUserPrincipal();
        
        Map<String, Object> permissions = new HashMap<>();
        permissions.put("authorities", user.getAuthorities());
        permissions.put("canCreateUsers", SecurityContextUtils.hasAuthority("USER_CREATE"));
        permissions.put("canApproveLoans", SecurityContextUtils.hasAuthority("LOAN_APPROVE"));
        permissions.put("isAdmin", SecurityContextUtils.hasRole("ADMIN"));
        permissions.put("isManager", SecurityContextUtils.hasRole("MANAGER"));
        
        return ResponseEntity.ok(permissions);
    }
}
```

### Conditional Logic Based on User Context

```java
@RestController
@RequestMapping("/api/reports")
public class ReportController {
    
    @GetMapping("/financial")
    public ResponseEntity<?> getFinancialReport() {
        String currentUserId = SecurityContextUtils.requireCurrentUserId();
        
        // Different data based on user role
        if (SecurityContextUtils.hasRole("ADMIN")) {
            // Admin sees all data
            return ResponseEntity.ok(reportService.getFullFinancialReport());
        } else if (SecurityContextUtils.hasRole("MANAGER")) {
            // Manager sees branch data
            String branchId = SecurityContextUtils.getCurrentUserBranchId()
                .orElseThrow(() -> new IllegalStateException("Manager must have branch"));
            return ResponseEntity.ok(reportService.getBranchFinancialReport(branchId));
        } else {
            // Regular users see limited data
            return ResponseEntity.ok(reportService.getUserFinancialSummary(currentUserId));
        }
    }
}
```

## Usage in Aspect-Oriented Programming (AOP)

### Custom Security Aspect

```java
@Aspect
@Component
public class SecurityAspect {
    
    @Before("@annotation(requiresBranchAccess)")
    public void checkBranchAccess(JoinPoint joinPoint, RequiresBranchAccess requiresBranchAccess) {
        String requiredBranchId = requiresBranchAccess.branchId();
        
        // Admin bypass
        if (SecurityContextUtils.hasRole("ADMIN")) {
            return;
        }
        
        // Check if user can access the required branch
        boolean canAccess = SecurityContextUtils.getCurrentUserBranchId()
            .map(userBranchId -> userBranchId.equals(requiredBranchId))
            .orElse(false);
        
        if (!canAccess) {
            String username = SecurityContextUtils.getCurrentUsername().orElse("unknown");
            throw new AccessDeniedException(
                String.format("User %s cannot access branch %s", username, requiredBranchId)
            );
        }
    }
}
```

## Audit Logging

### Comprehensive Audit Logging

```java
@Service
public class AuditService {
    
    public void logUserAction(String action, String resourceId, String details) {
        String auditInfo = SecurityContextUtils.getCurrentUserPrincipal()
            .map(user -> String.format(
                "User[id=%s, username=%s, email=%s, branch=%s]",
                user.getId(), user.getUsername(), user.getEmail(), user.getBranchId()
            ))
            .orElse("Anonymous");
        
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(action);
        auditLog.setResourceId(resourceId);
        auditLog.setUserInfo(auditInfo);
        auditLog.setDetails(details);
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setUserId(SecurityContextUtils.getCurrentUserId().orElse(null));
        
        auditLogRepository.save(auditLog);
        
        log.info("Audit: {} performed {} on {} - {}", auditInfo, action, resourceId, details);
    }
}
```

## Error Handling

### Graceful Error Handling

```java
@Service
public class UserService {
    
    public User getCurrentUserEntity() {
        try {
            String userId = SecurityContextUtils.requireCurrentUserId();
            return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        } catch (IllegalStateException e) {
            throw new AuthenticationRequiredException("User must be authenticated to access this resource");
        }
    }
    
    public Optional<String> getCurrentUserBranchSafely() {
        try {
            return SecurityContextUtils.getCurrentUserBranchId();
        } catch (Exception e) {
            log.warn("Failed to get current user branch: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
```

## Testing

### Unit Testing with SecurityContextUtils

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    void testGetCurrentUser_WithAuthentication() {
        // Mock SecurityContext
        UserPrincipal mockUser = createMockUserPrincipal();
        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(mockUser);
        when(mockAuth.isAuthenticated()).thenReturn(true);
        
        SecurityContext mockSecurityContext = mock(SecurityContext.class);
        when(mockSecurityContext.getAuthentication()).thenReturn(mockAuth);
        SecurityContextHolder.setContext(mockSecurityContext);
        
        // Test the service method
        Optional<String> userId = SecurityContextUtils.getCurrentUserId();
        assertTrue(userId.isPresent());
        assertEquals("user-123", userId.get());
    }
}
```

## Best Practices

### 1. Always Check Authentication Status
```java
// Good
if (SecurityContextUtils.isAuthenticated()) {
    String userId = SecurityContextUtils.requireCurrentUserId();
    // Process with user ID
}

// Better - use Optional methods
SecurityContextUtils.getCurrentUserId().ifPresent(userId -> {
    // Process with user ID
});
```

### 2. Use Appropriate Methods
```java
// Use Optional methods when authentication might not be present
Optional<String> userId = SecurityContextUtils.getCurrentUserId();

// Use require methods when authentication is mandatory
String userId = SecurityContextUtils.requireCurrentUserId(); // throws exception if not authenticated
```

### 3. Cache User Information When Needed
```java
@Service
public class CachedUserService {
    
    @Cacheable(value = "currentUser", key = "#root.methodName")
    public UserPrincipal getCurrentUserCached() {
        return SecurityContextUtils.requireCurrentUserPrincipal();
    }
}
```

### 4. Combine with Business Logic
```java
public boolean canEditResource(String resourceId) {
    // Check authentication first
    if (!SecurityContextUtils.isAuthenticated()) {
        return false;
    }
    
    // Admin can edit everything
    if (SecurityContextUtils.hasRole("ADMIN")) {
        return true;
    }
    
    // Check if user owns the resource
    String currentUserId = SecurityContextUtils.requireCurrentUserId();
    return resourceService.isOwner(resourceId, currentUserId);
}
```

## Available Methods Summary

| Method | Return Type | Description |
|--------|-------------|-------------|
| `getCurrentUserPrincipal()` | `Optional<UserPrincipal>` | Get current user principal |
| `getCurrentUserId()` | `Optional<String>` | Get current user ID |
| `getCurrentUsername()` | `Optional<String>` | Get current username |
| `getCurrentUserEmail()` | `Optional<String>` | Get current user email |
| `getCurrentUserBranchId()` | `Optional<String>` | Get current user's branch ID |
| `hasAuthority(String)` | `boolean` | Check if user has specific authority |
| `hasAnyAuthority(String...)` | `boolean` | Check if user has any of the authorities |
| `hasRole(String)` | `boolean` | Check if user has specific role |
| `hasAnyRole(String...)` | `boolean` | Check if user has any of the roles |
| `requireCurrentUserId()` | `String` | Get user ID (throws exception if not authenticated) |
| `requireCurrentUserPrincipal()` | `UserPrincipal` | Get user principal (throws exception if not authenticated) |
| `isAuthenticated()` | `boolean` | Check if user is authenticated |
| `getCurrentAuthentication()` | `Optional<Authentication>` | Get Spring Security Authentication object |

This utility class makes it easy to access current user information throughout your application while maintaining clean, readable code. 