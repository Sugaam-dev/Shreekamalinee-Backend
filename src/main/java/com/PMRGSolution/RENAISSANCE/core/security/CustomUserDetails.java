package com.PMRGSolution.RENAISSANCE.core.security;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.PMRGSolution.RENAISSANCE.features.auth.entity.User;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CustomUserDetails implements UserDetails {

    private final User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // "ROLE_" prefix is essential for hasRole() in SecurityConfig
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    // Helper method to get the ID directly in controllers or services
    public UUID getId() {
        return user.getId();
    }

    // REMOVED: getActiveSessionId() 
    // Logic: Session validation is now performed against the ActiveSession table in JwtAuthenticationFilter

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
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
        // Blocks users with enabled=false (unverified OTP)
        return user.isEnabled();
    }
}