package com.PMRGSolution.RENAISSANCE.core.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.PMRGSolution.RENAISSANCE.Constant.AuthProvider;
import com.PMRGSolution.RENAISSANCE.Constant.Role;
import com.PMRGSolution.RENAISSANCE.features.auth.entity.User;
import com.PMRGSolution.RENAISSANCE.features.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // 1. Use a single variable so they always match
        String adminEmail = "admin@example.com"; 

        // 2. Check the SAME email you are about to save
        if (userRepository.findByEmailIgnoreCase(adminEmail).isEmpty()) {
            User admin = User.builder()
                    .firstName("System")
                    .lastName("Admin")
                    .email(adminEmail) // Matches the check above
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .enabled(true)
                    .provider(AuthProvider.LOCAL)
                    .build();

            userRepository.save(admin);
            System.out.println(">>> SUCCESS: Admin account created and ENABLED.");
        } else {
            System.out.println(">>> INFO: Admin account already exists. Skipping.");
        }
    }
}