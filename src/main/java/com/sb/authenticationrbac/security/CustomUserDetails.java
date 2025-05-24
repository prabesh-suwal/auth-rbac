package com.sb.authenticationrbac.security;

import com.sb.authenticationrbac.entities.Branch;
import com.sb.authenticationrbac.entities.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

// Custom UserDetails implementation
public class CustomUserDetails implements UserDetails {
    private final String userId;
    private final String username;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final String  branch;
    
    public CustomUserDetails(User user) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.branch = user.getBranchId();
        this.authorities = user.getRoleIds().stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getBranch() {
        return branch;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
    
    @Override
    public String getPassword() {
        return password;
    }
    
    @Override
    public String getUsername() {
        return username;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
}