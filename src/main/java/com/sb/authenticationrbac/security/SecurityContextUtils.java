package com.sb.authenticationrbac.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Utility class for accessing the current authenticated user's information
 * from the Spring Security context.
 */
@Component
public class SecurityContextUtils {

    /**
     * Get the current authenticated UserPrincipal
     * 
     * @return Optional containing the UserPrincipal if authenticated, empty otherwise
     */
    public static Optional<UserPrincipal> getCurrentUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && 
            authentication.isAuthenticated() && 
            authentication.getPrincipal() instanceof UserPrincipal) {
            return Optional.of((UserPrincipal) authentication.getPrincipal());
        }
        
        return Optional.empty();
    }

    /**
     * Get the current authenticated user's ID
     * 
     * @return Optional containing the user ID if authenticated, empty otherwise
     */
    public static Optional<String> getCurrentUserId() {
        return getCurrentUserPrincipal().map(UserPrincipal::getId);
    }

    /**
     * Get the current authenticated user's username
     * 
     * @return Optional containing the username if authenticated, empty otherwise
     */
    public static Optional<String> getCurrentUsername() {
        return getCurrentUserPrincipal().map(UserPrincipal::getUsername);
    }

    /**
     * Get the current authenticated user's email
     * 
     * @return Optional containing the email if authenticated, empty otherwise
     */
    public static Optional<String> getCurrentUserEmail() {
        return getCurrentUserPrincipal().map(UserPrincipal::getEmail);
    }

    /**
     * Get the current authenticated user's branch ID
     * 
     * @return Optional containing the branch ID if authenticated, empty otherwise
     */
    public static Optional<String> getCurrentUserBranchId() {
        return getCurrentUserPrincipal().map(UserPrincipal::getBranchId);
    }

    /**
     * Check if the current user has a specific authority/permission
     * 
     * @param authority the authority/permission to check
     * @return true if the user has the authority, false otherwise
     */
    public static boolean hasAuthority(String authority) {
        return getCurrentUserPrincipal()
                .map(userPrincipal -> userPrincipal.getAuthorities().stream()
                        .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(authority)))
                .orElse(false);
    }

    /**
     * Check if the current user has any of the specified authorities/permissions
     * 
     * @param authorities the authorities/permissions to check
     * @return true if the user has any of the authorities, false otherwise
     */
    public static boolean hasAnyAuthority(String... authorities) {
        return getCurrentUserPrincipal()
                .map(userPrincipal -> {
                    for (String authority : authorities) {
                        if (userPrincipal.getAuthorities().stream()
                                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(authority))) {
                            return true;
                        }
                    }
                    return false;
                })
                .orElse(false);
    }

    /**
     * Check if the current user has a specific role
     * 
     * @param role the role to check (without ROLE_ prefix)
     * @return true if the user has the role, false otherwise
     */
    public static boolean hasRole(String role) {
        return hasAuthority("ROLE_" + role);
    }

    /**
     * Check if the current user has any of the specified roles
     * 
     * @param roles the roles to check (without ROLE_ prefix)
     * @return true if the user has any of the roles, false otherwise
     */
    public static boolean hasAnyRole(String... roles) {
        String[] roleAuthorities = new String[roles.length];
        for (int i = 0; i < roles.length; i++) {
            roleAuthorities[i] = "ROLE_" + roles[i];
        }
        return hasAnyAuthority(roleAuthorities);
    }

    /**
     * Get the current authenticated user's ID, throwing an exception if not authenticated
     * 
     * @return the user ID
     * @throws IllegalStateException if no user is authenticated
     */
    public static String requireCurrentUserId() {
        return getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException("No authenticated user found"));
    }

    /**
     * Get the current authenticated UserPrincipal, throwing an exception if not authenticated
     * 
     * @return the UserPrincipal
     * @throws IllegalStateException if no user is authenticated
     */
    public static UserPrincipal requireCurrentUserPrincipal() {
        return getCurrentUserPrincipal()
                .orElseThrow(() -> new IllegalStateException("No authenticated user found"));
    }

    /**
     * Check if there is currently an authenticated user
     * 
     * @return true if a user is authenticated, false otherwise
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && 
               authentication.isAuthenticated() && 
               authentication.getPrincipal() instanceof UserPrincipal;
    }

    /**
     * Get the current Spring Security Authentication object
     * 
     * @return Optional containing the Authentication if present, empty otherwise
     */
    public static Optional<Authentication> getCurrentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? Optional.of(authentication) : Optional.empty();
    }
} 