package com.sb.authenticationrbac.entities;

import lombok.Data;

import java.util.List;


@Data
public class BranchAccessConfig {
    private String type; // "OWN_BRANCH", "ALL_BRANCHES", "SPECIFIC_BRANCHES", "BRANCH_HIERARCHY"
    private List<String> allowedBranches; // Specific branch IDs if type is SPECIFIC_BRANCHES
    private boolean includeSubBranches = false; // For hierarchy-based access
    private String branchFieldPath = "branch.id"; // Path to branch field in resource
    
    // constructors, getters, setters
}
