package com.sb.authenticationrbac.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Main annotation for dynamic permission checking
 * All logic will be evaluated from stored configurations
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckPermission {
    
    /**
     * Permission identifier that will be looked up in the database
     */
    String value();
    
    /**
     * Resource type being accessed (e.g., "LOAN", "USER", "BRANCH")
     */
    String resource() default "";
    
    /**
     * Parameter name that contains the resource ID
     */
    String resourceIdParam() default "id";
    
    /**
     * Parameter name that contains the resource object
     */
    String resourceParam() default "";
    
    /**
     * Custom context parameters to pass to the permission evaluator
     */
    String[] contextParams() default {};
    
    /**
     * Custom error message
     */
    String message() default "Access denied";
    
    /**
     * Operation being performed (CREATE, READ, UPDATE, DELETE, APPROVE, etc.)
     */
    String operation() default "";
}
