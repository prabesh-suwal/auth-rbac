package com.sb.authenticationrbac.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for checking multiple permissions with dynamic logic
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckPermissions {
    CheckPermission[] value();
    
    /**
     * Logic for combining permissions: AND, OR
     */
    String logic() default "AND";
    
    String message() default "Access denied";
}