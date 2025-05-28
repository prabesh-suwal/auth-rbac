package com.sb.authenticationrbac.aop;

import com.sb.authenticationrbac.exceptions.PermissionDeniedException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple test to verify that PermissionDeniedException includes permission details
 */
class PermissionDeniedExceptionTest {

    @Test
    void permissionDeniedException_WithAllDetails_ShouldContainAllInformation() {
        // Arrange
        String message = "Access denied";
        String permissionName = "USER_CREATE";
        String resource = "User[id=123, name=Test User]";
        String operation = "CREATE";
        Map<String, Object> context = new HashMap<>();
        context.put("requestMethod", "POST");
        context.put("requestPath", "/api/v1/users");
        context.put("userId", "testuser123");

        // Act
        PermissionDeniedException exception = new PermissionDeniedException(
            message, permissionName, resource, operation, context
        );

        // Assert
        assertEquals(message, exception.getMessage());
        assertEquals(permissionName, exception.getPermissionName());
        assertEquals(resource, exception.getResource());
        assertEquals(operation, exception.getOperation());
        assertEquals(context, exception.getContext());
        
        // Verify none of the fields are null
        assertNotNull(exception.getPermissionName());
        assertNotNull(exception.getResource());
        assertNotNull(exception.getOperation());
        assertNotNull(exception.getContext());
        
        System.out.println("Exception details:");
        System.out.println("Message: " + exception.getMessage());
        System.out.println("Permission: " + exception.getPermissionName());
        System.out.println("Resource: " + exception.getResource());
        System.out.println("Operation: " + exception.getOperation());
        System.out.println("Context: " + exception.getContext());
    }

    @Test
    void permissionDeniedException_WithOnlyMessage_ShouldHaveNullDetails() {
        // Arrange
        String message = "Access denied";

        // Act
        PermissionDeniedException exception = new PermissionDeniedException(message);

        // Assert
        assertEquals(message, exception.getMessage());
        assertNull(exception.getPermissionName());
        assertNull(exception.getResource());
        assertNull(exception.getOperation());
        assertNull(exception.getContext());
    }

    @Test
    void permissionDeniedException_WithPartialDetails_ShouldContainProvidedInformation() {
        // Arrange
        String message = "Access denied";
        String permissionName = "USER_VIEW";
        String resource = null; // Null resource
        String operation = "READ";

        // Act
        PermissionDeniedException exception = new PermissionDeniedException(
            message, permissionName, resource, operation
        );

        // Assert
        assertEquals(message, exception.getMessage());
        assertEquals(permissionName, exception.getPermissionName());
        assertNull(exception.getResource()); // Should be null as provided
        assertEquals(operation, exception.getOperation());
        assertNull(exception.getContext()); // Should be null as not provided
    }
} 