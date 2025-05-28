# Spring Boot RBAC Authentication System

A comprehensive, enterprise-grade Role-Based Access Control (RBAC) system built with Spring Boot and MongoDB. This system provides dynamic, configuration-driven permission management with fine-grained access control, performance monitoring, and scalable architecture.

## 🚀 Features

### Core RBAC Features
- **JWT-Based Authentication**: Secure token-based authentication with refresh tokens
- **Dynamic Permission Evaluation**: Database-driven permission configuration without code changes
- **Multi-Layer Access Control**: Role-based, branch-based, amount-based, time-based, and resource-specific permissions
- **Hierarchical Branch Management**: Support for complex organizational structures
- **Temporary Permissions**: Time-limited access grants with custom conditions
- **User-Specific Overrides**: Individual permission customizations
- **SpEL Expression Support**: Complex business logic through Spring Expression Language

### Security Features
- **JWT Token Management**: Access and refresh token handling
- **Password Encryption**: BCrypt password hashing
- **CORS Configuration**: Cross-origin resource sharing support
- **Security Context Integration**: Spring Security integration
- **Authentication Entry Points**: Custom error handling for unauthorized access

### Performance & Scalability
- **Comprehensive Caching**: Multi-level caching for optimal performance
- **Performance Monitoring**: Real-time metrics and analytics
- **Audit Trail**: Complete permission evaluation logging
- **Horizontal Scaling**: Stateless design for cloud deployment

### Enterprise Features
- **JSON Configuration**: Permission definitions through JSON files
- **Validation & Integrity**: Branch hierarchy validation and consistency checks
- **Monitoring Dashboard**: Performance metrics and system health endpoints
- **Error Handling**: Graceful degradation and detailed error reporting

## 🏗️ Architecture

### Core Components

```
┌─────────────────────────────────────────────────────────────┐
│                    Authentication Flow                      │
├─────────────────────────────────────────────────────────────┤
│ Login → JWT Token → Security Context → Permission Check    │
│                                                             │
│ 1. User Authentication   4. Resource Loading               │
│ 2. JWT Token Generation  5. Permission Evaluation          │
│ 3. Security Context      6. Access Decision                │
└─────────────────────────────────────────────────────────────┘
```

### Permission Evaluation Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    Permission Evaluation Flow               │
├─────────────────────────────────────────────────────────────┤
│ @CheckPermission → Aspect → Service → Multi-Layer Checks   │
│                                                             │
│ 1. Role-Based Access     5. Custom Conditions              │
│ 2. Branch Access         6. Validation Rules               │
│ 3. Amount Limits         7. User Overrides                 │
│ 4. Time Restrictions     8. Temporary Permissions          │
└─────────────────────────────────────────────────────────────┘
```

### Key Services
- **AuthController**: Authentication endpoints (login, register, refresh)
- **DynamicPermissionEvaluationService**: Core permission evaluation engine
- **BranchHierarchyService**: Organizational structure management
- **PermissionPerformanceMonitoringService**: Performance tracking and analytics
- **CustomUserDetailsService**: User authentication and authorization

## 📋 Prerequisites

- Java 17+
- MongoDB 4.4+
- Spring Boot 3.x
- Maven 3.6+

## 🛠️ Installation

1. **Clone the repository**
```bash
git clone <repository-url>
cd authentication-rbac
```

2. **Configure MongoDB**
```yaml
# application.properties
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/rbac_db
```

3. **Configure JWT**
```yaml
# application.properties
app:
  jwt:
    secret: mySecretKey123456789012345678901234567890
    expiration: 86400 # 24 hours
    refresh-expiration: 604800 # 7 days
```

4. **Build and run**
```bash
mvn clean install
mvn spring-boot:run
```

## 🔐 Authentication

### Default Users
The system creates default users on startup:

| Username | Password | Role | Description |
|----------|----------|------|-------------|
| admin | admin123 | ADMIN | System Administrator |
| manager | manager123 | MANAGER | Branch Manager |
| loanofficer | loan123 | LOAN_OFFICER | Loan Officer |
| usermanager | usermgr123 | USER_MANAGER | User Manager |

### Authentication Endpoints

#### Login
```bash
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "type": "Bearer",
  "id": "user-admin",
  "username": "admin",
  "email": "admin@company.com",
  "branchId": "branch-head-office",
  "roles": ["ROLE_ADMIN", "ADMIN", "USER_CREATE", "SYSTEM_ADMIN", ...]
}
```

#### Register
```bash
POST /api/auth/register
Content-Type: application/json

{
  "username": "newuser",
  "email": "newuser@company.com",
  "password": "password123",
  "branchId": "branch-001",
  "roleIds": ["role-loan-officer"]
}
```

#### Refresh Token
```bash
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

#### Get Current User
```bash
GET /api/auth/me
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

### Using JWT Tokens

Include the JWT token in the Authorization header:
```bash
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

## 🧪 Testing Authentication

### Test Endpoints

The system provides test endpoints to verify authentication and authorization:

```bash
# Public endpoint (no authentication required)
GET /api/test/public

# Authenticated endpoint (requires valid JWT)
GET /api/test/authenticated
Authorization: Bearer <token>

# Admin role required
GET /api/test/admin
Authorization: Bearer <admin-token>

# Specific permission required
GET /api/test/user-create
Authorization: Bearer <token-with-user-create-permission>

# Multiple permissions (OR logic)
GET /api/test/multiple-permissions
Authorization: Bearer <token>
```

### Example Test Flow

1. **Login as admin**:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

2. **Test authenticated endpoint**:
```bash
curl -X GET http://localhost:8080/api/test/authenticated \
  -H "Authorization: Bearer <token-from-login>"
```

3. **Test permission-based endpoint**:
```bash
curl -X GET http://localhost:8080/api/test/user-create \
  -H "Authorization: Bearer <token-from-login>"
```

## 🔧 Configuration

### Permission Definitions
Create permission definitions in `src/main/resources/permission-definitions.json`:

```json
{
  "apiGroups": {
    "loanManagement": {
      "description": "Loan Management APIs",
      "permissions": [
        {
          "name": "LOAN_APPROVE",
          "resource": "LOAN",
          "operation": "APPROVE",
          "description": "Permission to approve loans with various conditions",
          "config": {
            "branchAccess": {
              "type": "OWN_BRANCH",
              "includeSubBranches": false
            },
            "amountLimit": {
              "enabled": true,
              "limitType": "ROLE_BASED",
              "roleLimits": {
                "LOAN_OFFICER": 100000.0,
                "MANAGER": 1000000.0
              }
            },
            "timeAccess": {
              "enabled": true,
              "allowedDays": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"],
              "allowedTimeWindows": [
                {"startTime": "09:00", "endTime": "17:00"}
              ]
            },
            "conditions": [
              "#resource.status == 'PENDING'",
              "#resource.amount > 0"
            ]
          }
        }
      ]
    }
  }
}
```

### Role Definitions
Define roles in `src/main/resources/role-definitions.json`:

```json
{
  "roles": [
    {
      "name": "LOAN_OFFICER",
      "description": "Basic loan processing officer",
      "permissions": ["LOAN_VIEW", "LOAN_CREATE", "LOAN_UPDATE"],
      "configuration": {
        "defaultAmountLimits": {
          "LOAN_APPROVE": 100000.0
        },
        "workingHours": {
          "enabled": true,
          "startTime": "09:00",
          "endTime": "17:00"
        }
      }
    }
  ]
}
```

## 💻 Usage

### Basic Permission Check
```java
@RestController
public class LoanController {
    
    @PostMapping("/loans/{id}/approve")
    @CheckPermission(
        value = "LOAN_APPROVE",
        resource = "LOAN",
        resourceIdParam = "id",
        operation = "APPROVE",
        message = "Loan approval not authorized"
    )
    public ResponseEntity<Loan> approveLoan(@PathVariable String id) {
        // Your business logic here
        return ResponseEntity.ok(loanService.approve(id));
    }
}
```

### Multiple Permission Checks
```java
@PostMapping("/loans/{id}/emergency-approve")
@CheckPermissions(
    value = {
        @CheckPermission(value = "LOAN_APPROVE", operation = "APPROVE"),
        @CheckPermission(value = "EMERGENCY_OVERRIDE", operation = "OVERRIDE")
    },
    logic = "OR",
    message = "Emergency approval requires special permissions"
)
public ResponseEntity<Loan> emergencyApprove(@PathVariable String id) {
    return ResponseEntity.ok(loanService.emergencyApprove(id));
}
```

### User Creation with Complex Permissions
```java
@PostMapping("/api/v1/users")
@CheckPermission(value = "USER_CREATE", operation = "CREATE")
public ResponseEntity<User> createUser(@Valid @RequestBody CreateUserRequest request) {
    User user = userService.createUser(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(user);
}
```

Example request:
```json
{
  "username": "john.doe",
  "email": "john.doe@company.com",
  "branchId": "branch-001",
  "roleIds": ["role-loan-officer"],
  "permissionConfig": {
    "branchAccessOverride": {
      "type": "SPECIFIC_BRANCHES",
      "allowedBranches": ["branch-001", "branch-002"],
      "includeSubBranches": true
    },
    "amountLimitOverrides": {
      "LOAN_APPROVE": 150000.0
    },
    "temporaryPermissions": [
      {
        "permissionId": "EMERGENCY_APPROVE",
        "grantedAt": "2024-01-01T09:00:00Z",
        "expiresAt": "2024-01-31T17:00:00Z",
        "grantedBy": "admin-user-id",
        "reason": "Temporary emergency approval authority"
      }
    ]
  }
}
```

## 📊 Monitoring & Analytics

### Performance Monitoring
Access performance metrics at `/api/admin/monitoring/performance`:

```json
{
  "totalEvaluations": 15420,
  "averageEvaluationTimeMs": 12.5,
  "cacheHitRate": 0.85,
  "topSlowPermissions": [
    {
      "permissionName": "LOAN_APPROVE",
      "averageExecutionTime": 25.3,
      "totalEvaluations": 1250
    }
  ],
  "mostFrequentPermissions": [
    {
      "permissionName": "LOAN_VIEW",
      "totalEvaluations": 8500,
      "cacheHitRate": 0.92
    }
  ]
}
```

### System Health
Check system health at `/api/admin/monitoring/health`:

```json
{
  "status": "UP",
  "cache": {
    "totalCaches": 12,
    "cacheNames": ["permissionEvaluations", "branches", "users"]
  },
  "performance": {
    "totalEvaluations": 15420,
    "averageExecutionTime": 12.5,
    "cacheHitRate": 0.85
  },
  "branchHierarchy": {
    "valid": true,
    "orphanedBranches": 0,
    "cyclicBranches": 0
  }
}
```

## 🔒 Security Features

### JWT Security
- **Secure Token Generation**: HMAC SHA-512 signing
- **Token Expiration**: Configurable access and refresh token lifetimes
- **Refresh Token Rotation**: New tokens on refresh
- **Token Validation**: Comprehensive token validation

### Branch-Based Access Control
```java
// Users can only access resources from their branch or sub-branches
"branchAccess": {
  "type": "BRANCH_HIERARCHY",
  "includeSubBranches": true
}
```

### Amount-Based Limits
```java
// Different approval limits based on user roles
"amountLimit": {
  "enabled": true,
  "limitType": "ROLE_BASED",
  "roleLimits": {
    "LOAN_OFFICER": 100000.0,
    "MANAGER": 1000000.0,
    "SENIOR_MANAGER": 5000000.0
  }
}
```

### Time-Based Restrictions
```java
// Access only during business hours
"timeAccess": {
  "enabled": true,
  "allowedDays": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"],
  "allowedTimeWindows": [
    {"startTime": "09:00", "endTime": "17:00"}
  ],
  "timezone": "Asia/Kathmandu"
}
```

### Custom Business Logic
```java
// Complex conditions using SpEL
"conditions": [
  "#resource.status == 'PENDING'",
  "#resource.amount <= 1000000 or #user.roles.?[name=='SENIOR_MANAGER'].size() > 0",
  "#resource.riskScore <= 70"
]
```

## 🚀 Performance Optimization

### Caching Strategy
- **Permission Evaluations**: Cached based on user, permission, and operation
- **Branch Hierarchy**: Cached branch relationships and access patterns
- **User/Role Data**: Cached frequently accessed user and role information
- **JWT Token Validation**: Cached token validation results

### Performance Metrics
- Real-time evaluation time tracking
- Cache hit/miss ratios
- Permission usage analytics
- User activity monitoring

## 🧪 Testing

Run the test suite:
```bash
mvn test
```

### Test Coverage
- Unit tests for all permission evaluation logic
- Integration tests for API endpoints
- Security tests for authentication and authorization
- Performance tests for scalability validation

## 📚 API Documentation

### Core Endpoints

#### Authentication
- `POST /api/auth/login` - User login
- `POST /api/auth/register` - User registration
- `POST /api/auth/refresh` - Refresh JWT token
- `POST /api/auth/logout` - User logout
- `GET /api/auth/me` - Get current user profile

#### User Management
- `POST /api/v1/users` - Create user with permissions
- `GET /api/v1/users/{id}` - Get user details
- `PUT /api/v1/users/{id}` - Update user

#### Permission Management
- `POST /api/admin/permissions/permissions` - Create permission
- `PUT /api/admin/permissions/roles/{roleId}/config` - Update role configuration
- `POST /api/admin/permissions/users/{userId}/roles/{roleId}` - Assign role
- `POST /api/admin/permissions/users/{userId}/amount-limits` - Set amount limits

#### Monitoring
- `GET /api/admin/monitoring/performance` - Performance metrics
- `GET /api/admin/monitoring/health` - System health
- `GET /api/admin/monitoring/cache/status` - Cache status
- `POST /api/admin/monitoring/cache/clear` - Clear caches

#### Test Endpoints
- `GET /api/test/public` - Public endpoint (no auth)
- `GET /api/test/authenticated` - Requires authentication
- `GET /api/test/admin` - Requires ADMIN role
- `GET /api/test/user-create` - Requires USER_CREATE permission

## 🔧 Advanced Configuration

### Custom Permission Evaluators
Extend the system with custom evaluators:

```java
@Component
public class CustomPermissionEvaluator {
    
    public boolean evaluateCustomCondition(User user, Object resource, Map<String, Object> context) {
        // Your custom logic here
        return true;
    }
}
```

### Performance Tuning
```yaml
# application.properties
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=10000,expireAfterWrite=30m
      
rbac:
  performance:
    monitoring:
      enabled: true
      metrics-retention-days: 30

app:
  jwt:
    secret: your-very-secure-secret-key-here
    expiration: 86400
    refresh-expiration: 604800
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

For support and questions:
- Create an issue in the repository
- Check the [documentation](docs/)
- Review the [FAQ](docs/FAQ.md)

## 🗺️ Roadmap

- [ ] Redis cache integration
- [ ] GraphQL API support
- [ ] Advanced analytics dashboard
- [ ] Multi-tenant support
- [ ] OAuth2/OIDC integration
- [ ] Kubernetes deployment templates
- [ ] Rate limiting and throttling
- [ ] Advanced audit logging

---

**Built with ❤️ for enterprise security and scalability** 