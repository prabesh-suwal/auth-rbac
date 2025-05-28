package com.sb.authenticationrbac;

import com.sb.authenticationrbac.entities.*;
import com.sb.authenticationrbac.repositories.RoleRepository;
import com.sb.authenticationrbac.repositories.UserRepository;
import com.sb.authenticationrbac.role.dto.RoleConfiguration;
import com.sb.authenticationrbac.services.BranchAccessHierarchyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class demonstrating the Branch Access Hierarchy System
 *
 * Tests the three-tier hierarchy:
 * 1. User-specific overrides (highest priority)
 * 2. Role-level configuration (medium priority)
 * 3. Permission-level configuration (lowest priority)
 */
@SpringBootTest
@ActiveProfiles("test")
public class BranchAccessHierarchyTest {

    @Autowired
    private BranchAccessHierarchyService branchAccessHierarchyService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User rm1; // RM of Branch 1
    private User rm2; // RM of Branch 2
    private User admin; // Admin user
    private Role relationshipManagerRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        // Create test roles
        setupRoles();

        // Create test users
        setupUsers();
    }

    private void setupRoles() {
        // Create RELATIONSHIP_MANAGER role with cross-branch access
        relationshipManagerRole = new Role();
        relationshipManagerRole.setId("rm-role-id");
        relationshipManagerRole.setName("RELATIONSHIP_MANAGER");
        relationshipManagerRole.setDescription("Relationship Manager with cross-branch access");

        RoleConfiguration rmConfig = new RoleConfiguration();
        RoleConfiguration.BranchRestrictions rmBranchRestrictions = new RoleConfiguration.BranchRestrictions();
        rmBranchRestrictions.setType("OWN_BRANCH_ONLY");
        rmBranchRestrictions.setAllowCrossBranchView(true); // This enables cross-branch access
        rmConfig.setBranchRestrictions(rmBranchRestrictions);
        relationshipManagerRole.setConfiguration(rmConfig);
        relationshipManagerRole.setActive(true);

        roleRepository.save(relationshipManagerRole);

        // Create ADMIN role with all branches access
        adminRole = new Role();
        adminRole.setId("admin-role-id");
        adminRole.setName("ADMIN");
        adminRole.setDescription("Administrator with all branches access");

        RoleConfiguration adminConfig = new RoleConfiguration();
        RoleConfiguration.BranchRestrictions adminBranchRestrictions = new RoleConfiguration.BranchRestrictions();
        adminBranchRestrictions.setType("ALL_BRANCHES");
        adminBranchRestrictions.setAllowCrossBranchView(true);
        adminConfig.setBranchRestrictions(adminBranchRestrictions);
        adminRole.setConfiguration(adminConfig);
        adminRole.setActive(true);

        roleRepository.save(adminRole);
    }

    private void setupUsers() {
        // Create RM1 (Branch 1)
        rm1 = new User();
        rm1.setId("rm1");
        rm1.setUsername("rm1");
        rm1.setEmail("rm1@bank.com");
        rm1.setBranchId("branch1");
        rm1.setRoleIds(Arrays.asList(relationshipManagerRole.getId()));
        rm1.setActive(true);
        userRepository.save(rm1);

        // Create RM2 (Branch 2)
        rm2 = new User();
        rm2.setId("rm2");
        rm2.setUsername("rm2");
        rm2.setEmail("rm2@bank.com");
        rm2.setBranchId("branch2");
        rm2.setRoleIds(Arrays.asList(relationshipManagerRole.getId()));
        rm2.setActive(true);
        userRepository.save(rm2);

        // Create Admin
        admin = new User();
        admin.setId("admin");
        admin.setUsername("admin");
        admin.setEmail("admin@bank.com");
        admin.setBranchId("branch1");
        admin.setRoleIds(Arrays.asList(adminRole.getId()));
        admin.setActive(true);
        userRepository.save(admin);
    }

    @Test
    void testCrossBranchScenario_RM2_AccessingBranch1() {
        // Test the main scenario: RM2 (Branch 2) creating loan for Branch 1
        BranchAccessHierarchyService.CrossBranchScenarioResult result =
            branchAccessHierarchyService.demonstrateCrossBranchScenario("rm2", "branch1", "LOAN_CREATE");

        // Should be successful due to role-level cross-branch access
        assertTrue(result.isSuccess(), "RM2 should be able to access Branch 1 due to role configuration");
        assertTrue(result.getReason().contains("cross-branch"), "Reason should mention cross-branch access");
        assertFalse(result.getScenarioSteps().isEmpty(), "Should have evaluation steps");

        System.out.println("Cross-Branch Scenario Result:");
        System.out.println("Success: " + result.isSuccess());
        System.out.println("Reason: " + result.getReason());
        result.getScenarioSteps().forEach(step -> System.out.println("  " + step));
    }

    @Test
    void testOwnBranchAccess_RM1_AccessingBranch1() {
        // Test standard scenario: RM1 accessing their own branch
        BranchAccessHierarchyService.BranchAccessAnalysis analysis =
            branchAccessHierarchyService.analyzeBranchAccess("rm1", "branch1", "LOAN_CREATE", null);

        assertTrue(analysis.isAccessGranted(), "RM1 should be able to access their own branch");

        System.out.println("\nOwn Branch Access Result:");
        System.out.println("Access Granted: " + analysis.isAccessGranted());
        System.out.println("Reason: " + analysis.getFinalReason());
        analysis.getEvaluationSteps().forEach(step -> System.out.println("  " + step));
    }

    @Test
    void testAdminAccess_AllBranches() {
        // Test admin accessing any branch
        BranchAccessHierarchyService.BranchAccessAnalysis analysis =
            branchAccessHierarchyService.analyzeBranchAccess("admin", "branch2", "LOAN_CREATE", null);

        assertTrue(analysis.isAccessGranted(), "Admin should be able to access any branch");
        assertTrue(analysis.getFinalReason().contains("ALL_BRANCHES"), "Should mention ALL_BRANCHES access");

        System.out.println("\nAdmin Access Result:");
        System.out.println("Access Granted: " + analysis.isAccessGranted());
        System.out.println("Reason: " + analysis.getFinalReason());
    }

    @Test
    void testUserSpecificOverride_HighestPriority() {
        // Configure user-specific override for RM2
        BranchAccessConfig userOverride = new BranchAccessConfig();
        userOverride.setType("SPECIFIC_BRANCHES");
        userOverride.setAllowedBranches(Arrays.asList("branch1", "branch3"));

        branchAccessHierarchyService.configureUserBranchAccess("rm2", userOverride);

        // Test access to allowed branch
        BranchAccessHierarchyService.BranchAccessAnalysis analysis1 =
            branchAccessHierarchyService.analyzeBranchAccess("rm2", "branch1", "LOAN_CREATE", null);
        assertTrue(analysis1.isAccessGranted(), "Should be granted due to user-specific override");
        assertTrue(analysis1.getFinalReason().contains("User granted specific branch access"), "Should mention user-specific override");

        // Test access to non-allowed branch
        BranchAccessHierarchyService.BranchAccessAnalysis analysis2 =
            branchAccessHierarchyService.analyzeBranchAccess("rm2", "branch4", "LOAN_CREATE", null);
        assertFalse(analysis2.isAccessGranted(), "Should be denied as branch4 not in allowed list");

        System.out.println("\nUser-Specific Override Results:");
        System.out.println("Branch 1 Access: " + analysis1.isAccessGranted() + " - " + analysis1.getFinalReason());
        System.out.println("Branch 4 Access: " + analysis2.isAccessGranted() + " - " + analysis2.getFinalReason());
    }

    @Test
    void testRoleConfiguration_MediumPriority() {
        // Test role-level configuration without user-specific overrides
        BranchAccessHierarchyService.BranchAccessAnalysis analysis =
            branchAccessHierarchyService.analyzeBranchAccess("rm2", "branch1", "LOAN_CREATE", null);

        assertTrue(analysis.isAccessGranted(), "Should be granted due to role-level configuration");
        assertTrue(analysis.getFinalReason().contains("cross-branch view access"), "Should mention role-level access");

        System.out.println("\nRole-Level Configuration Result:");
        System.out.println("Access Granted: " + analysis.isAccessGranted());
        System.out.println("Reason: " + analysis.getFinalReason());
    }

    @Test
    void testUserBranchAccessSummary() {
        // Get comprehensive summary for RM2
        BranchAccessHierarchyService.UserBranchAccessSummary summary =
            branchAccessHierarchyService.getUserBranchAccessSummary("rm2");

        assertNotNull(summary, "Summary should not be null");
        assertEquals("rm2", summary.getUserId());
        assertEquals("branch2", summary.getUserBranchId());
        assertNotNull(summary.getRoleBranchConfigurations(), "Role configurations should not be null");
        assertTrue(summary.getRoleBranchConfigurations().containsKey("RELATIONSHIP_MANAGER"),
                  "Should contain RELATIONSHIP_MANAGER role configuration");

        System.out.println("\nUser Branch Access Summary for RM2:");
        System.out.println("User ID: " + summary.getUserId());
        System.out.println("User Branch: " + summary.getUserBranchId());
        System.out.println("Role Configurations: " + summary.getRoleBranchConfigurations().keySet());
    }

    @Test
    void testHierarchyPriority() {
        // Configure user-specific override that contradicts role configuration
        BranchAccessConfig userOverride = new BranchAccessConfig();
        userOverride.setType("OWN_BRANCH"); // More restrictive than role

        branchAccessHierarchyService.configureUserBranchAccess("rm2", userOverride);

        // Test cross-branch access - should be denied due to user-specific override
        BranchAccessHierarchyService.BranchAccessAnalysis analysis =
            branchAccessHierarchyService.analyzeBranchAccess("rm2", "branch1", "LOAN_CREATE", null);

        assertFalse(analysis.isAccessGranted(), "Should be denied due to user-specific override taking priority");
        assertTrue(analysis.getFinalReason().contains("User restricted to own branch only"), "Should mention user-specific override");

        System.out.println("\nHierarchy Priority Test:");
        System.out.println("Access Granted: " + analysis.isAccessGranted());
        System.out.println("Reason: " + analysis.getFinalReason());
        System.out.println("This demonstrates user-specific overrides have highest priority");
    }

    @Test
    void testRoleConfigurationUpdate() {
        // Test updating role configuration
        branchAccessHierarchyService.configureRoleBranchAccess(
            relationshipManagerRole.getId(),
            "ALL_BRANCHES",
            true
        );

        // Verify the change took effect
        BranchAccessHierarchyService.BranchAccessAnalysis analysis =
            branchAccessHierarchyService.analyzeBranchAccess("rm1", "branch2", "LOAN_CREATE", null);

        assertTrue(analysis.isAccessGranted(), "Should be granted due to updated role configuration");
        assertTrue(analysis.getFinalReason().contains("ALL_BRANCHES"), "Should mention ALL_BRANCHES access");

        System.out.println("\nRole Configuration Update Test:");
        System.out.println("Access Granted: " + analysis.isAccessGranted());
        System.out.println("Reason: " + analysis.getFinalReason());
    }

    @Test
    void testMultipleUsersScenario() {
        // Test scenario with multiple users
        List<String> userIds = Arrays.asList("rm1", "rm2", "admin");
        String targetBranch = "branch1";

        System.out.println("\nMultiple Users Scenario - Target Branch: " + targetBranch);

        for (String userId : userIds) {
            BranchAccessHierarchyService.BranchAccessAnalysis analysis =
                branchAccessHierarchyService.analyzeBranchAccess(userId, targetBranch, "LOAN_CREATE", null);

            System.out.println(String.format("User %s: %s - %s",
                userId,
                analysis.isAccessGranted() ? "GRANTED" : "DENIED",
                analysis.getFinalReason()));
        }
    }
}