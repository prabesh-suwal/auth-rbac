package com.sb.authenticationrbac.security;

import com.sb.authenticationrbac.entities.Permission;
import com.sb.authenticationrbac.entities.Role;
import com.sb.authenticationrbac.entities.User;
import com.sb.authenticationrbac.repositories.RoleRepository;
import com.sb.authenticationrbac.repositories.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public CustomUserDetailsService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameAndActive(username, true)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return createUserPrincipal(user);
    }

    @Transactional
    public UserDetails loadUserById(String id) {
        User user = userRepository.findByIdAndActive(id, true)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + id));

        return createUserPrincipal(user);
    }

    private UserPrincipal createUserPrincipal(User user) {
        List<String> permissions = getUserPermissions(user);
        return UserPrincipal.create(user, permissions);
    }

    private List<String> getUserPermissions(User user) {
        // Get user roles
        List<Role> userRoles = roleRepository.findByIdInAndActive(user.getRoleIds(), true);
        
        // Extract all permissions from roles
        Set<String> permissions = userRoles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());

        // Add role names as authorities (with ROLE_ prefix for Spring Security)
        userRoles.forEach(role -> {
            permissions.add("ROLE_" + role.getName());
            permissions.add(role.getName()); // Also add without prefix for our custom checks
        });

        return permissions.stream().collect(Collectors.toList());
    }
} 