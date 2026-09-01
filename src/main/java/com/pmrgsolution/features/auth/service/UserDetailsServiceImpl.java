package com.pmrgsolution.features.auth.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pmrgsolution.core.security.CustomUserDetails;
import com.pmrgsolution.features.auth.entity.User;
import com.pmrgsolution.features.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

   @Override
@Transactional(readOnly = true)
public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    // Trim and Lowercase for 100% consistency across all features
    String cleanEmail = email.trim().toLowerCase();
    
    return userRepository.findByEmailIgnoreCase(cleanEmail)
            .map(CustomUserDetails::new)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + cleanEmail));
}
}