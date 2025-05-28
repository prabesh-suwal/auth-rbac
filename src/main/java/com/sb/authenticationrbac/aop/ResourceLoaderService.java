package com.sb.authenticationrbac.aop;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

@Service
public class ResourceLoaderService {

    private final MongoTemplate mongoTemplate;

    public ResourceLoaderService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Object loadResource(String resourceType, Object resourceId) {
        if (resourceType == null || resourceId == null) {
            return null;
        }

        try {
            // Convert resource type to entity class
            Class<?> entityClass = getEntityClass(resourceType);
            if (entityClass == null) {
                return null;
            }

            // Load the resource from MongoDB
            return mongoTemplate.findById(resourceId, entityClass);
        } catch (Exception e) {
            // Log error and return null
            return null;
        }
    }

    private Class<?> getEntityClass(String resourceType) {
        try {
            // Map resource types to entity classes
            switch (resourceType.toUpperCase()) {
                case "USER":
                    return Class.forName("com.sb.authenticationrbac.entities.User");
                case "ROLE":
                    return Class.forName("com.sb.authenticationrbac.entities.Role");
                case "PERMISSION":
                    return Class.forName("com.sb.authenticationrbac.entities.Permission");
                case "BRANCH":
                    return Class.forName("com.sb.authenticationrbac.entities.Branch");
                case "LOAN":
                    return Class.forName("com.sb.authenticationrbac.entities.Loan");
                default:
                    // Try to construct the class name dynamically
                    String className = "com.sb.authenticationrbac.entities." + 
                                     resourceType.substring(0, 1).toUpperCase() + 
                                     resourceType.substring(1).toLowerCase();
                    return Class.forName(className);
            }
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
