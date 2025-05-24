package com.sb.authenticationrbac.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.Map;

@ControllerAdvice
public class DynamicPermissionExceptionHandler {

    private final Logger logger = LoggerFactory.getLogger(DynamicPermissionExceptionHandler.class);

    @ExceptionHandler(PermissionDeniedException.class)
    public ResponseEntity<ErrorResponse> handlePermissionDenied(PermissionDeniedException ex) {
        logger.warn("Permission denied: {} - Permission: {}, Resource: {}, Operation: {}", 
                   ex.getMessage(), ex.getPermissionName(), ex.getResource(), ex.getOperation());
        
        ErrorResponse error = new ErrorResponse(
            "PERMISSION_DENIED",
            ex.getMessage(),
            HttpStatus.FORBIDDEN.value(),
            Map.of(
                "permission", ex.getPermissionName(),
                "resource", ex.getResource(),
                "operation", ex.getOperation(),
                "timestamp", LocalDateTime.now()
            )
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler({UserNotFoundException.class, RoleNotFoundException.class, 
                      PermissionNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleResourceNotFound(RuntimeException ex) {
        ErrorResponse error = new ErrorResponse(
            "RESOURCE_NOT_FOUND",
            ex.getMessage(),
            HttpStatus.NOT_FOUND.value()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        logger.error("Unexpected error during permission evaluation", ex);
        
        ErrorResponse error = new ErrorResponse(
            "INTERNAL_ERROR",
            "An unexpected error occurred",
            HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}