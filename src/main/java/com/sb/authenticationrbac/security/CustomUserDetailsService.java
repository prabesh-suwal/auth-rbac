package com.sb.authenticationrbac.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Placeholder implementation.
        // In a real application, you would load user details from a database or other source.
        throw new UsernameNotFoundException("User not found with username: " + username);
    }
}
