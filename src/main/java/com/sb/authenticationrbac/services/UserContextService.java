package com.sb.authenticationrbac.services;

import com.sb.authenticationrbac.entities.User;
import com.sb.authenticationrbac.repositories.UserRepository;
import com.sb.authenticationrbac.security.SecurityContextUtils;
import com.sb.authenticationrbac.security.UserPrincipal;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service that demonstrates how to use SecurityContextUtils
 * to access current user information in your business logic.
 */
@Service
public class UserContextService {

    private final UserRepository userRepository;

    public UserContextService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Get the current authenticated user's full User entity from database
     * 
     * @return Optional containing the User entity if authenticated, empty otherwise
     */
    public Optional<User> getCurrentUser() {
        return SecurityContextUtils.getCurrentUserId()
                .flatMap(userRepository::findById);
    }

    /**
     * Get the current authenticated user's UserPrincipal
     * 
     * @return Optional containing the UserPrincipal if authenticated, empty otherwise
     */
    public Optional<UserPrincipal> getCurrentUserPrincipal() {
        return SecurityContextUtils.getCurrentUserPrincipal();
    }

    /**
     * Get the current user's ID
     * 
     * @return Optional containing the user ID if authenticated, empty otherwise
     */
    public Optional<String> getCurrentUserId() {
        return SecurityContextUtils.getCurrentUserId();
    }

    /**
     * Get the current user's branch ID
     * 
     * @return Optional containing the branch ID if authenticated, empty otherwise
     */
    public Optional<String> getCurrentUserBranchId() {
        return SecurityContextUtils.getCurrentUserBranchId();
    }

    /**
     * Check if the current user has a specific permission
     * 
     * @param permission the permission to check
     * @return true if the user has the permission, false otherwise
     */
    public boolean hasPermission(String permission) {
        return SecurityContextUtils.hasAuthority(permission);
    }

    /**
     * Check if the current user has a specific role
     * 
     * @param role the role to check (without ROLE_ prefix)
     * @return true if the user has the role, false otherwise
     */
    public boolean hasRole(String role) {
        return SecurityContextUtils.hasRole(role);
    }

    /**
     * Check if the current user can access a specific branch
     * This is an example of business logic using current user context
     * 
     * @param branchId the branch ID to check access for
     * @return true if the user can access the branch, false otherwise
     */
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

    /**
     * Get user information for audit logging
     * 
     * @return formatted string with user information for logging
     */
    public String getCurrentUserForAudit() {
        return SecurityContextUtils.getCurrentUserPrincipal()
                .map(user -> String.format("User[id=%s, username=%s, branch=%s]", 
                        user.getId(), user.getUsername(), user.getBranchId()))
                .orElse("Anonymous");
    }

    /**
     * Example method that requires authentication
     * Throws exception if no user is authenticated
     * 
     * @return the current user's information
     */
    public String getRequiredUserInfo() {
        UserPrincipal user = SecurityContextUtils.requireCurrentUserPrincipal();
        return String.format("Authenticated user: %s (ID: %s, Branch: %s)", 
                user.getUsername(), user.getId(), user.getBranchId());
    }

    /**
     * Example method for checking multiple permissions
     * 
     * @return true if user can manage users (has any user management permission)
     */
    public boolean canManageUsers() {
        return SecurityContextUtils.hasAnyAuthority(
                "USER_CREATE", "USER_UPDATE", "USER_DELETE", "ROLE_ADMIN"
        );
    }

    /**
     * Example method for role-based access
     * 
     * @return true if user is admin or manager
     */
    public boolean isAdminOrManager() {
        return SecurityContextUtils.hasAnyRole("ADMIN", "MANAGER");
    }
} 