package com.sb.authenticationrbac.services;

import com.sb.authenticationrbac.dtos.requests.CreateUserRequest;
import com.sb.authenticationrbac.entities.*;
import com.sb.authenticationrbac.exception.ValidationException;
import com.sb.authenticationrbac.repositories.BranchRepository;
import com.sb.authenticationrbac.repositories.RoleRepository;
import com.sb.authenticationrbac.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       BranchRepository branchRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.branchRepository = branchRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User createUser(CreateUserRequest request) {
        // Validate unique constraints
        validateUniqueConstraints(request);

        // Validate branch exists
//        if (!branchRepository.existsById(request.getBranchId())) {
//            throw new ValidationException("Branch with ID " + request.getBranchId() + " does not exist");
//        }

        // Validate roles exist
        request.getRoleIds().forEach(roleId -> {
            if (!roleRepository.existsById(roleId)) {
                throw new ValidationException("Role with ID " + roleId + " does not exist");
            }
        });

        // Create user entity
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setUsername(request.getUsername());
        user.setPassword(this.passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setBranchId(request.getBranchId());
        user.setRoleIds(request.getRoleIds());
        user.setActive(request.isActive());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        // Map permission config if present
        if (request.getPermissionConfig() != null) {
            UserPermissionConfig config = new UserPermissionConfig();
            
            if (request.getPermissionConfig().getBranchAccessOverride() != null) {
                BranchAccessConfig branchAccess = new BranchAccessConfig();
                branchAccess.setType(request.getPermissionConfig().getBranchAccessOverride().getType());
                branchAccess.setAllowedBranches(request.getPermissionConfig().getBranchAccessOverride().getAllowedBranches());
                branchAccess.setIncludeSubBranches(request.getPermissionConfig().getBranchAccessOverride().isIncludeSubBranches());
                config.setBranchAccessOverride(branchAccess);
            }

            config.setAmountLimitOverrides(request.getPermissionConfig().getAmountLimitOverrides());
            
            if (request.getPermissionConfig().getResourceAccesses() != null) {
                config.setResourceAccesses(request.getPermissionConfig().getResourceAccesses().stream()
                    .map(ra -> {
                        ResourceAccess access = new ResourceAccess();
                        access.setResourceType(ra.getResourceType());
                        access.setResourceId(ra.getResourceId());
                        access.setAccessType(ra.getAccessType());
                        access.setAllowedOperations(ra.getAllowedOperations());
                        access.setExpiresAt(ra.getExpiresAt() != null ? 
                            LocalDateTime.ofInstant(ra.getExpiresAt(), ZoneId.systemDefault()) : null);
                        return access;
                    })
                    .collect(Collectors.toList()));
            }

            if (request.getPermissionConfig().getTemporaryPermissions() != null) {
                config.setTemporaryPermissions(request.getPermissionConfig().getTemporaryPermissions().stream()
                    .map(tp -> {
                        TemporaryPermission temp = new TemporaryPermission();
                        temp.setPermissionId(tp.getPermissionId());
                        temp.setGrantedAt(LocalDateTime.ofInstant(tp.getGrantedAt(), ZoneId.systemDefault()));
                        temp.setExpiresAt(LocalDateTime.ofInstant(tp.getExpiresAt(), ZoneId.systemDefault()));
                        temp.setGrantedBy(tp.getGrantedBy());
                        temp.setReason(tp.getReason());
                        temp.setConditions(tp.getConditions());
                        return temp;
                    })
                    .collect(Collectors.toList()));
            }

            config.setCustomConditions(request.getPermissionConfig().getCustomConditions());
            user.setPermissionConfig(config);
        }

        return userRepository.save(user);
    }

    private void validateUniqueConstraints(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ValidationException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("Email already exists");
        }
    }
} 