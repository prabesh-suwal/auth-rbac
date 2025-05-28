package com.sb.authenticationrbac.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Arrays;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    @Primary
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        
        // Define cache names for different components
        cacheManager.setCacheNames(Arrays.asList(
            // Permission evaluation caches
            "permissionEvaluations",
            "userPermissions",
            "rolePermissions",
            
            // Branch hierarchy caches
            "branches",
            "branchHierarchy",
            "subBranches",
            "parentBranches",
            "branchHierarchyTree",
            "accessibleBranches",
            
            // User and role caches
            "users",
            "roles",
            "userRoles",
            
            // Permission configuration caches
            "permissions",
            "permissionConfigs",
            
            // Resource caches
            "resources",
            "resourceAccess",
            
            // Validation caches
            "validationResults",
            "spelExpressions"
        ));
        
        return cacheManager;
    }
} 