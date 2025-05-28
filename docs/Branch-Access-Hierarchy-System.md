# Branch Access Hierarchy System

## Overview

The Branch Access Hierarchy System implements a three-tier access control mechanism that allows fine-grained control over branch-based permissions. This system enables complex scenarios like cross-branch operations while maintaining security and auditability.

## Hierarchy Levels (Priority Order)

### 1. User-Specific Overrides (Highest Priority)
Individual user configurations that override all other settings.

**Configuration Location**: `User.permissionConfig.branchAccessOverride`

**Available Types**:
- `ALL_BRANCHES`: User can access all branches
- `OWN_BRANCH`: User restricted to their own branch only
- `SPECIFIC_BRANCHES`: User can access only specified branches
- `INHERIT`: Continue to role-level evaluation

**Example Configuration**:
```json
{
  "userId": "rm2",
  "permissionConfig": {
    "branchAccessOverride": {
      "type": "SPECIFIC_BRANCHES",
      "allowedBranches": ["branch1", "branch2", "branch3"],
      "branchFieldPath": "branch.id"
    }
  }
}
```

### 2. Role-Level Configuration (Medium Priority)
Role-based branch access rules that can override permission restrictions.

**Configuration Location**: `Role.configuration.branchRestrictions`

**Available Types**:
- `ALL_BRANCHES`: Role grants access to all branches
- `OWN_BRANCH_AND_SUBORDINATES`: Role grants access to own branch and subordinates
- `OWN_BRANCH_ONLY`: Role restricted to own branch (with optional cross-branch view)

**Cross-Branch Options**:
- `allowCrossBranchView`: When `true`, allows cross-branch operations even with `OWN_BRANCH_ONLY`

**Example Configuration**:
```json
{
  "name": "RELATIONSHIP_MANAGER",
  "configuration": {
    "branchRestrictions": {
      "type": "OWN_BRANCH_ONLY",
      "allowCrossBranchView": true
    }
  }
}
```

### 3. Permission-Level Configuration (Lowest Priority)
Default permission-based access control.

**Configuration Location**: `Permission.config.branchAccess`

**Available Types**:
- `ALL_BRANCHES`: Permission allows all branches
- `OWN_BRANCH`: Permission restricted to own branch
- `SPECIFIC_BRANCHES`: Permission allows specific branches only
- `BRANCH_HIERARCHY`: Permission follows branch hierarchy

**Example Configuration**:
```json
{
  "name": "LOAN_CREATE",
  "config": {
    "branchAccess": {
      "type": "OWN_BRANCH",
      "branchFieldPath": "branch.id"
    }
  }
}
```

## Cross-Branch Scenario Solution

### Problem Statement
**Scenario**: Customer goes to Branch 2, asks RM2 to create loan for Branch 1.
- rm1: Relationship Manager of Branch 1
- rm2: Relationship Manager of Branch 2
- Customer: Wants loan from Branch 1 but is at Branch 2
- rm2: Needs to create loan on behalf of Branch 1

### Solution Implementation

#### Step 1: Configure Role-Level Cross-Branch Access
Update the RELATIONSHIP_MANAGER role to allow cross-branch operations:

```json
{
  "name": "RELATIONSHIP_MANAGER",
  "configuration": {
    "branchRestrictions": {
      "type": "OWN_BRANCH_ONLY",
      "allowCrossBranchView": true
    }
  }
}
```

#### Step 2: Permission Evaluation Flow
When rm2 tries to create a loan for Branch 1:

1. **User-Specific Check**: No user-specific overrides → Continue
2. **Role-Level Check**: RELATIONSHIP_MANAGER has `allowCrossBranchView: true` → **GRANT ACCESS**
3. **Permission-Level Check**: Not reached (role-level granted access)

#### Step 3: Result
✅ **Access Granted**: "Role-level: Cross-branch view access via RELATIONSHIP_MANAGER role"

## API Endpoints

### Test Cross-Branch Scenario
```http
GET /api/admin/branch-access-hierarchy/demo/cross-branch/{rmUserId}/{targetBranchId}?permissionName=LOAN_CREATE
```

**Example**:
```http
GET /api/admin/branch-access-hierarchy/demo/cross-branch/rm2/branch1?permissionName=LOAN_CREATE
```

**Response**:
```json
{
  "scenario": "Cross-Branch Access Test",
  "rmUserId": "rm2",
  "targetBranchId": "branch1",
  "permissionName": "LOAN_CREATE",
  "accessGranted": true,
  "reason": "Role-level: Cross-branch view access via RELATIONSHIP_MANAGER role",
  "evaluationSteps": [
    "Scenario: RM from branch2 attempting to access branch1",
    "STEP 1 - User-specific overrides: No user-specific overrides configured",
    "STEP 2 - Role-level configuration: Role RELATIONSHIP_MANAGER grants cross-branch view access"
  ]
}
```

### Analyze Branch Access Hierarchy
```http
GET /api/admin/branch-access-hierarchy/analyze/{userId}/{targetBranchId}?permissionName=LOAN_CREATE
```

### Configure User-Specific Branch Access
```http
POST /api/admin/branch-access-hierarchy/configure/user/{userId}/branch-access
Content-Type: application/json

{
  "type": "SPECIFIC_BRANCHES",
  "allowedBranches": ["branch1", "branch2"],
  "branchFieldPath": "branch.id",
  "includeSubBranches": false
}
```

### Configure Role-Level Branch Access
```http
POST /api/admin/branch-access-hierarchy/configure/role/{roleId}/branch-access
Content-Type: application/json

{
  "branchRestrictionType": "OWN_BRANCH_ONLY",
  "allowCrossBranchView": true
}
```

## Real-World Examples

### Example 1: Standard RM Operations
**User**: rm1 (Branch 1)
**Action**: Create loan for Branch 1
**Result**: ✅ Allowed (own branch access)

### Example 2: Cross-Branch RM Operations
**User**: rm2 (Branch 2)
**Action**: Create loan for Branch 1
**Result**: ✅ Allowed (role-level cross-branch access)

### Example 3: Admin Operations
**User**: admin (Any branch)
**Action**: Create loan for any branch
**Result**: ✅ Allowed (role-level ALL_BRANCHES access)

### Example 4: Restricted User
**User**: clerk1 (Branch 1)
**Action**: Create loan for Branch 2
**Result**: ❌ Denied (no cross-branch access)

## Configuration Scenarios

### Scenario 1: Enable Cross-Branch for All RMs
```json
{
  "name": "RELATIONSHIP_MANAGER",
  "configuration": {
    "branchRestrictions": {
      "type": "ALL_BRANCHES",
      "allowCrossBranchView": true
    }
  }
}
```

### Scenario 2: Selective Cross-Branch Access
```json
{
  "userId": "rm2",
  "permissionConfig": {
    "branchAccessOverride": {
      "type": "SPECIFIC_BRANCHES",
      "allowedBranches": ["branch1", "branch2"]
    }
  }
}
```

### Scenario 3: Temporary Cross-Branch Access
```json
{
  "userId": "rm3",
  "permissionConfig": {
    "temporaryPermissions": [
      {
        "permissionId": "LOAN_CREATE",
        "expiresAt": "2024-12-31T23:59:59",
        "conditions": {
          "allowedBranches": ["branch1"]
        }
      }
    ]
  }
}
```

## Security Considerations

### Audit Trail
All branch access evaluations are logged with:
- User ID and branch
- Target branch
- Permission name
- Hierarchy level that granted/denied access
- Timestamp and reason

### Principle of Least Privilege
- Default to most restrictive (permission-level)
- Explicit grants at higher levels
- Clear hierarchy prevents conflicts

### Role-Based Security
- Cross-branch access tied to business roles
- Easy to revoke by role modification
- Supports organizational hierarchy

## Performance Optimization

### Caching Strategy
- User role configurations cached
- Branch hierarchy cached
- Permission evaluations cached with user+permission+branch key

### Evaluation Short-Circuiting
- Stop at first decisive result
- User-specific overrides evaluated first
- Role-level evaluation only if needed

## Monitoring and Analytics

### Key Metrics
- Cross-branch access frequency
- Role-level override usage
- User-specific override usage
- Access denial reasons

### Available Endpoints
```http
GET /api/admin/branch-access-hierarchy/summary/{userId}
GET /api/admin/branch-access-hierarchy/documentation
POST /api/admin/branch-access-hierarchy/test/scenario
```

## Best Practices

### 1. Role-Level Configuration
- Use role-level configuration for business-driven cross-branch access
- Prefer `allowCrossBranchView: true` over `ALL_BRANCHES` for RMs
- Document business justification for cross-branch roles

### 2. User-Specific Overrides
- Use sparingly for exceptional cases
- Set expiration dates when possible
- Regular audit of user-specific overrides

### 3. Permission-Level Configuration
- Keep as default restrictive baseline
- Use for standard same-branch operations
- Avoid frequent changes to permission-level config

### 4. Testing and Validation
- Test cross-branch scenarios before deployment
- Use hierarchy analysis endpoints for troubleshooting
- Monitor access patterns for anomalies

## Implementation Details

### Code Structure
- `DynamicPermissionEvaluationService`: Core evaluation logic
- `BranchAccessHierarchyService`: Hierarchy management and testing
- `BranchAccessHierarchyController`: REST API endpoints
- `RoleConfiguration.BranchRestrictions`: Role-level configuration
- `UserPermissionConfig.branchAccessOverride`: User-level configuration

### Database Schema
```javascript
// User collection
{
  "_id": "rm2",
  "branchId": "branch2",
  "permissionConfig": {
    "branchAccessOverride": {
      "type": "SPECIFIC_BRANCHES",
      "allowedBranches": ["branch1", "branch2"]
    }
  }
}

// Role collection
{
  "_id": "role123",
  "name": "RELATIONSHIP_MANAGER",
  "configuration": {
    "branchRestrictions": {
      "type": "OWN_BRANCH_ONLY",
      "allowCrossBranchView": true
    }
  }
}
```

This hierarchy system provides a flexible, secure, and auditable solution for complex branch access scenarios while maintaining clear business logic and easy configuration management. 