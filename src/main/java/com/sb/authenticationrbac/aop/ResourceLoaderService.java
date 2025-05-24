package com.sb.authenticationrbac.aop;

import com.sb.authenticationrbac.entities.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

// Service for loading resources dynamically
@Service
public class ResourceLoaderService {
    
    private final MongoTemplate mongoTemplate;
    private final Map<String, Class<?>> resourceTypeMap;
    
    public ResourceLoaderService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
        this.resourceTypeMap = initializeResourceTypeMap();
    }
    
    private Map<String, Class<?>> initializeResourceTypeMap() {
        Map<String, Class<?>> typeMap = new HashMap<>();
//        typeMap.put("LOAN", Loan.class);
        typeMap.put("USER", User.class);
        typeMap.put("BRANCH", Branch.class);
        typeMap.put("ROLE", Role.class);
        typeMap.put("PERMISSION", Permission.class);
        // Add other resource types as needed
        return typeMap;
    }
    
    public Object loadResource(String resourceType, Object resourceId) {
        Class<?> resourceClass = resourceTypeMap.get(resourceType.toUpperCase());
        if (resourceClass == null) {
            return null;
        }
        
        try {
            return mongoTemplate.findById(resourceId, resourceClass);
        } catch (Exception e) {
            // Log error and return null
            return null;
        }
    }
    
    public void registerResourceType(String typeName, Class<?> resourceClass) {
        resourceTypeMap.put(typeName.toUpperCase(), resourceClass);
    }
}
