package com.sb.authenticationrbac.services;

import com.sb.authenticationrbac.entities.*;
import com.sb.authenticationrbac.repositories.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class DataInitializationService implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializationService(UserRepository userRepository,
                                   RoleRepository roleRepository,
                                   PermissionRepository permissionRepository,
                                   BranchRepository branchRepository,
                                   PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.branchRepository = branchRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        initializeDefaultData();
    }

    private void initializeDefaultData() {


        // Create default branch if not exists
        if (branchRepository.count() == 0) {
            createDefaultBranches();
        }

        // Create default permissions if not exists
        if (permissionRepository.count() == 0) {
            createDefaultPermissions();
        }

        // Create default roles if not exists
        if (roleRepository.count() == 0) {
            createDefaultRoles();
        }

        // Create default users if not exists
        if (userRepository.count() == 0) {
            createDefaultUsers();
        }
    }

    private void createDefaultBranches() {
        Branch headOffice = new Branch();
        headOffice.setId("branch-head-office");
        headOffice.setName("Head Office");
        headOffice.setCode("HO");
        headOffice.setAddress("Main Street, City Center");
        headOffice.setActive(true);
        headOffice.setCreatedAt(LocalDateTime.now());
        headOffice.setUpdatedAt(LocalDateTime.now());
        branchRepository.save(headOffice);

        Branch branch1 = new Branch();
        branch1.setId("branch-001");
        branch1.setName("Branch 001");
        branch1.setCode("B001");
        branch1.setAddress("Downtown Branch");
        branch1.setParentBranchId("branch-head-office");
        branch1.setActive(true);
        branch1.setCreatedAt(LocalDateTime.now());
        branch1.setUpdatedAt(LocalDateTime.now());
        branchRepository.save(branch1);

        Branch branch2 = new Branch();
        branch2.setId("branch-002");
        branch2.setName("Branch 002");
        branch2.setCode("B002");
        branch2.setAddress("Uptown Branch");
        branch2.setParentBranchId("branch-head-office");
        branch2.setActive(true);
        branch2.setCreatedAt(LocalDateTime.now());
        branch2.setUpdatedAt(LocalDateTime.now());
        branchRepository.save(branch2);
    }

    private void createDefaultPermissions() {
        List<Permission> permissions = Arrays.asList(
            createPermission("USER_CREATE", "Create users"),
            createPermission("USER_VIEW", "View users"),
            createPermission("USER_UPDATE", "Update users"),
            createPermission("USER_DELETE", "Delete users"),
            createPermission("ROLE_MANAGE", "Manage roles"),
            createPermission("PERMISSION_MANAGE", "Manage permissions"),
            createPermission("SYSTEM_ADMIN", "System administration"),
            createPermission("SYSTEM_MONITOR", "System monitoring"),
            createPermission("LOAN_CREATE", "Create loans"),
            createPermission("LOAN_VIEW", "View loans"),
            createPermission("LOAN_UPDATE", "Update loans"),
            createPermission("LOAN_APPROVE", "Approve loans"),
            createPermission("LOAN_REJECT", "Reject loans"),
            createPermission("EMERGENCY_OVERRIDE", "Emergency override"),
            createPermission("SUPER_ADMIN_ACCESS", "Super admin access")
        );

        permissionRepository.saveAll(permissions);
    }

    private Permission createPermission(String name, String description) {
        Permission permission = new Permission();
        permission.setName(name);
        permission.setDescription(description);
        permission.setActive(true);
        permission.setCreatedAt(LocalDateTime.now());
        permission.setUpdatedAt(LocalDateTime.now());
        return permission;
    }

    private void createDefaultRoles() {
        // Get all permissions
        List<Permission> allPermissions = permissionRepository.findAll();

        // Create ADMIN role with all permissions
        Role adminRole = new Role();
        adminRole.setId("role-admin");
        adminRole.setName("ADMIN");
        adminRole.setDescription("System Administrator");
        adminRole.setPermissions(allPermissions);
        adminRole.setActive(true);
        adminRole.setCreatedAt(LocalDateTime.now());
        adminRole.setUpdatedAt(LocalDateTime.now());
        roleRepository.save(adminRole);

        // Create USER_MANAGER role
        List<Permission> userManagerPermissions = allPermissions.stream()
                .filter(p -> p.getName().startsWith("USER_") || p.getName().equals("SYSTEM_MONITOR"))
                .toList();
        
        Role userManagerRole = new Role();
        userManagerRole.setId("role-user-manager");
        userManagerRole.setName("USER_MANAGER");
        userManagerRole.setDescription("User Manager");
        userManagerRole.setPermissions(userManagerPermissions);
        userManagerRole.setActive(true);
        userManagerRole.setCreatedAt(LocalDateTime.now());
        userManagerRole.setUpdatedAt(LocalDateTime.now());
        roleRepository.save(userManagerRole);

        // Create LOAN_OFFICER role
        List<Permission> loanOfficerPermissions = allPermissions.stream()
                .filter(p -> p.getName().startsWith("LOAN_") && !p.getName().equals("LOAN_APPROVE"))
                .toList();
        
        Role loanOfficerRole = new Role();
        loanOfficerRole.setId("role-loan-officer");
        loanOfficerRole.setName("LOAN_OFFICER");
        loanOfficerRole.setDescription("Loan Officer");
        loanOfficerRole.setPermissions(loanOfficerPermissions);
        loanOfficerRole.setActive(true);
        loanOfficerRole.setCreatedAt(LocalDateTime.now());
        loanOfficerRole.setUpdatedAt(LocalDateTime.now());
        roleRepository.save(loanOfficerRole);

        // Create MANAGER role
        List<Permission> managerPermissions = allPermissions.stream()
                .filter(p -> p.getName().startsWith("LOAN_") || p.getName().equals("USER_VIEW") || p.getName().equals("SYSTEM_MONITOR"))
                .toList();
        
        Role managerRole = new Role();
        managerRole.setId("role-manager");
        managerRole.setName("MANAGER");
        managerRole.setDescription("Branch Manager");
        managerRole.setPermissions(managerPermissions);
        managerRole.setActive(true);
        managerRole.setCreatedAt(LocalDateTime.now());
        managerRole.setUpdatedAt(LocalDateTime.now());
        roleRepository.save(managerRole);
    }

    private void createDefaultUsers() {
        // Create admin user
        User admin = new User();
        admin.setId("user-admin");
        admin.setUsername("admin");
        admin.setEmail("admin@company.com");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setBranchId("branch-head-office");
        admin.setRoleIds(Arrays.asList("role-admin"));
        admin.setActive(true);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        userRepository.save(admin);

        // Create manager user
        User manager = new User();
        manager.setId("user-manager");
        manager.setUsername("manager");
        manager.setEmail("manager@company.com");
        manager.setPassword(passwordEncoder.encode("manager123"));
        manager.setBranchId("branch-001");
        manager.setRoleIds(Arrays.asList("role-manager"));
        manager.setActive(true);
        manager.setCreatedAt(LocalDateTime.now());
        manager.setUpdatedAt(LocalDateTime.now());
        userRepository.save(manager);

        // Create loan officer user
        User loanOfficer = new User();
        loanOfficer.setId("user-loan-officer");
        loanOfficer.setUsername("loanofficer");
        loanOfficer.setEmail("loanofficer@company.com");
        loanOfficer.setPassword(passwordEncoder.encode("loan123"));
        loanOfficer.setBranchId("branch-001");
        loanOfficer.setRoleIds(Arrays.asList("role-loan-officer"));
        loanOfficer.setActive(true);
        loanOfficer.setCreatedAt(LocalDateTime.now());
        loanOfficer.setUpdatedAt(LocalDateTime.now());
        userRepository.save(loanOfficer);

        // Create user manager
        User userManager = new User();
        userManager.setId("user-user-manager");
        userManager.setUsername("usermanager");
        userManager.setEmail("usermanager@company.com");
        userManager.setPassword(passwordEncoder.encode("usermgr123"));
        userManager.setBranchId("branch-head-office");
        userManager.setRoleIds(Arrays.asList("role-user-manager"));
        userManager.setActive(true);
        userManager.setCreatedAt(LocalDateTime.now());
        userManager.setUpdatedAt(LocalDateTime.now());
        userRepository.save(userManager);

        System.out.println("=== Default Users Created ===");
        System.out.println("Admin: username=admin, password=admin123");
        System.out.println("Manager: username=manager, password=manager123");
        System.out.println("Loan Officer: username=loanofficer, password=loan123");
        System.out.println("User Manager: username=usermanager, password=usermgr123");
        System.out.println("===============================");
    }
} 