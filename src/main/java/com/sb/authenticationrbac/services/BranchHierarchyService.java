package com.sb.authenticationrbac.services;

import com.sb.authenticationrbac.entities.Branch;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

// Service for handling branch hierarchy
@Service
public class BranchHierarchyService {
    
    private final MongoTemplate mongoTemplate;
    
    public BranchHierarchyService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }
    
    public boolean hasAccessToBranch(String userBranchId, String targetBranchId, boolean includeSubBranches) {
        if (userBranchId.equals(targetBranchId)) {
            return true;
        }
        
        if (includeSubBranches) {
            return isSubBranch(userBranchId, targetBranchId);
        }
        
        return false;
    }
    
    private boolean isSubBranch(String parentBranchId, String childBranchId) {
        Branch childBranch = mongoTemplate.findById(childBranchId, Branch.class);
        
        while (childBranch != null && childBranch.getParentBranchId() != null) {
            if (childBranch.getParentBranchId().equals(parentBranchId)) {
                return true;
            }
            childBranch = mongoTemplate.findById(childBranch.getParentBranchId(), Branch.class);
        }
        
        return false;
    }
}