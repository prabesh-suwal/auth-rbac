package com.sb.authenticationrbac.services;

import com.sb.authenticationrbac.entities.Branch;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

// Service for handling branch hierarchy
@Service
public class BranchHierarchyService {
    
    private final MongoTemplate mongoTemplate;
    
    public BranchHierarchyService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }
    
    /**
     * Check if a user has access to a specific branch based on hierarchy
     */
    public boolean hasAccessToBranch(String userBranchId, String targetBranchId, boolean includeSubBranches) {
        if (userBranchId == null || targetBranchId == null) {
            return false;
        }

        // Direct branch match
        if (userBranchId.equals(targetBranchId)) {
            return true;
        }

        if (!includeSubBranches) {
            return false;
        }

        // Check if target branch is a sub-branch of user's branch
        return isSubBranch(userBranchId, targetBranchId);
    }
    
    /**
     * Check if targetBranchId is a sub-branch of parentBranchId
     */
    @Cacheable(value = "branchHierarchy", key = "#parentBranchId + '_' + #targetBranchId")
    public boolean isSubBranch(String parentBranchId, String targetBranchId) {
        if (parentBranchId == null || targetBranchId == null) {
            return false;
        }

        if (parentBranchId.equals(targetBranchId)) {
            return true;
        }

        Branch targetBranch = mongoTemplate.findById(targetBranchId, Branch.class);
        if (targetBranch == null || targetBranch.getParentBranchId() == null) {
            return false;
        }

        // Recursive check up the hierarchy
        return isSubBranch(parentBranchId, targetBranch.getParentBranchId());
    }
    
    /**
     * Get all sub-branches of a given branch (including nested sub-branches)
     */
    @Cacheable(value = "subBranches", key = "#branchId")
    public Set<String> getAllSubBranches(String branchId) {
        Set<String> subBranches = new HashSet<>();
        collectSubBranches(branchId, subBranches);
        return subBranches;
    }

    private void collectSubBranches(String branchId, Set<String> subBranches) {
        Branch branch = mongoTemplate.findById(branchId, Branch.class);
        if (branch == null || branch.getChildBranchIds() == null) {
            return;
        }

        for (String childBranchId : branch.getChildBranchIds()) {
            if (!subBranches.contains(childBranchId)) {
                subBranches.add(childBranchId);
                collectSubBranches(childBranchId, subBranches); // Recursive call
            }
        }
    }
    
    /**
     * Get all branches that a user can access based on their branch and hierarchy rules
     */
    public Set<String> getAccessibleBranches(String userBranchId, boolean includeSubBranches, boolean includeParentBranches) {
        Set<String> accessibleBranches = new HashSet<>();
        accessibleBranches.add(userBranchId); // Always include own branch

        if (includeSubBranches) {
            accessibleBranches.addAll(getAllSubBranches(userBranchId));
        }

        if (includeParentBranches) {
            accessibleBranches.addAll(getAllParentBranches(userBranchId));
        }

        return accessibleBranches;
    }
    
    /**
     * Get all parent branches of a given branch
     */
    @Cacheable(value = "parentBranches", key = "#branchId")
    public Set<String> getAllParentBranches(String branchId) {
        Set<String> parentBranches = new HashSet<>();
        collectParentBranches(branchId, parentBranches);
        return parentBranches;
    }

    private void collectParentBranches(String branchId, Set<String> parentBranches) {
        Branch branch = mongoTemplate.findById(branchId, Branch.class);
        if (branch == null || branch.getParentBranchId() == null) {
            return;
        }

        String parentBranchId = branch.getParentBranchId();
        if (!parentBranches.contains(parentBranchId)) {
            parentBranches.add(parentBranchId);
            collectParentBranches(parentBranchId, parentBranches); // Recursive call
        }
    }
    
    /**
     * Get the complete branch hierarchy tree
     */
    @Cacheable(value = "branchHierarchyTree")
    public Map<String, BranchNode> getBranchHierarchyTree() {
        List<Branch> allBranches = mongoTemplate.findAll(Branch.class);
        Map<String, BranchNode> branchMap = new HashMap<>();

        // Create nodes for all branches
        for (Branch branch : allBranches) {
            BranchNode node = new BranchNode();
            node.setBranchId(branch.getId());
            node.setBranchName(branch.getName());
            node.setBranchCode(branch.getCode());
            node.setParentBranchId(branch.getParentBranchId());
            node.setChildren(new ArrayList<>());
            branchMap.put(branch.getId(), node);
        }

        // Build parent-child relationships
        for (BranchNode node : branchMap.values()) {
            if (node.getParentBranchId() != null) {
                BranchNode parent = branchMap.get(node.getParentBranchId());
                if (parent != null) {
                    parent.getChildren().add(node);
                }
            }
        }

        return branchMap;
    }
    
    /**
     * Get root branches (branches with no parent)
     */
    public List<BranchNode> getRootBranches() {
        Map<String, BranchNode> hierarchyTree = getBranchHierarchyTree();
        return hierarchyTree.values().stream()
                .filter(node -> node.getParentBranchId() == null)
                .collect(Collectors.toList());
    }
    
    /**
     * Get the depth of a branch in the hierarchy (root = 0)
     */
    public int getBranchDepth(String branchId) {
        Branch branch = mongoTemplate.findById(branchId, Branch.class);
        if (branch == null) {
            return -1;
        }

        int depth = 0;
        String currentBranchId = branchId;

        while (currentBranchId != null) {
            Branch currentBranch = mongoTemplate.findById(currentBranchId, Branch.class);
            if (currentBranch == null || currentBranch.getParentBranchId() == null) {
                break;
            }
            depth++;
            currentBranchId = currentBranch.getParentBranchId();
        }

        return depth;
    }
    
    /**
     * Check if two branches are in the same hierarchy tree
     */
    public boolean areInSameHierarchy(String branchId1, String branchId2) {
        String root1 = getRootBranch(branchId1);
        String root2 = getRootBranch(branchId2);
        return root1 != null && root1.equals(root2);
    }
    
    /**
     * Get the root branch of a given branch
     */
    private String getRootBranch(String branchId) {
        String currentBranchId = branchId;

        while (currentBranchId != null) {
            Branch currentBranch = mongoTemplate.findById(currentBranchId, Branch.class);
            if (currentBranch == null) {
                return null;
            }

            if (currentBranch.getParentBranchId() == null) {
                return currentBranchId;
            }

            currentBranchId = currentBranch.getParentBranchId();
        }

        return null;
    }
    
    /**
     * Validate branch hierarchy (detect cycles, orphaned branches, etc.)
     */
    public BranchHierarchyValidationResult validateHierarchy() {
        BranchHierarchyValidationResult result = new BranchHierarchyValidationResult();
        List<Branch> allBranches = mongoTemplate.findAll(Branch.class);
        Set<String> allBranchIds = allBranches.stream().map(Branch::getId).collect(Collectors.toSet());

        for (Branch branch : allBranches) {
            // Check for orphaned parent references
            if (branch.getParentBranchId() != null && !allBranchIds.contains(branch.getParentBranchId())) {
                result.getOrphanedBranches().add(branch.getId());
            }

            // Check for cycles
            if (hasCycle(branch.getId(), new HashSet<>())) {
                result.getCyclicBranches().add(branch.getId());
            }

            // Check for inconsistent child references
            if (branch.getChildBranchIds() != null) {
                for (String childId : branch.getChildBranchIds()) {
                    Branch child = mongoTemplate.findById(childId, Branch.class);
                    if (child == null || !branch.getId().equals(child.getParentBranchId())) {
                        result.getInconsistentReferences().add(branch.getId() + " -> " + childId);
                    }
                }
            }
        }

        result.setValid(result.getOrphanedBranches().isEmpty() && 
                       result.getCyclicBranches().isEmpty() && 
                       result.getInconsistentReferences().isEmpty());

        return result;
    }

    private boolean hasCycle(String branchId, Set<String> visited) {
        if (visited.contains(branchId)) {
            return true;
        }

        visited.add(branchId);
        Branch branch = mongoTemplate.findById(branchId, Branch.class);

        if (branch != null && branch.getParentBranchId() != null) {
            return hasCycle(branch.getParentBranchId(), visited);
        }

        return false;
    }

    // Inner classes for hierarchy representation
    public static class BranchNode {
        private String branchId;
        private String branchName;
        private String branchCode;
        private String parentBranchId;
        private List<BranchNode> children;

        // Getters and setters
        public String getBranchId() { return branchId; }
        public void setBranchId(String branchId) { this.branchId = branchId; }
        public String getBranchName() { return branchName; }
        public void setBranchName(String branchName) { this.branchName = branchName; }
        public String getBranchCode() { return branchCode; }
        public void setBranchCode(String branchCode) { this.branchCode = branchCode; }
        public String getParentBranchId() { return parentBranchId; }
        public void setParentBranchId(String parentBranchId) { this.parentBranchId = parentBranchId; }
        public List<BranchNode> getChildren() { return children; }
        public void setChildren(List<BranchNode> children) { this.children = children; }
    }

    public static class BranchHierarchyValidationResult {
        private boolean valid;
        private List<String> orphanedBranches = new ArrayList<>();
        private List<String> cyclicBranches = new ArrayList<>();
        private List<String> inconsistentReferences = new ArrayList<>();

        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public List<String> getOrphanedBranches() { return orphanedBranches; }
        public void setOrphanedBranches(List<String> orphanedBranches) { this.orphanedBranches = orphanedBranches; }
        public List<String> getCyclicBranches() { return cyclicBranches; }
        public void setCyclicBranches(List<String> cyclicBranches) { this.cyclicBranches = cyclicBranches; }
        public List<String> getInconsistentReferences() { return inconsistentReferences; }
        public void setInconsistentReferences(List<String> inconsistentReferences) { this.inconsistentReferences = inconsistentReferences; }
    }
}