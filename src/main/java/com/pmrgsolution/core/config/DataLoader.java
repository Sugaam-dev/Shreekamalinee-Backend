package com.pmrgsolution.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.pmrgsolution.constant.AuthProvider;
import com.pmrgsolution.constant.Role;
import com.pmrgsolution.features.auth.entity.User;
import com.pmrgsolution.features.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@shreekamalinee.com}")
    private String adminEmail;

    @Value("${app.admin.password:Admin@ShreeKamalinee2026}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            log.warn("Initial Admin credentials not configured in environment. Skipping admin creation.");
            return;
        }

        String normalizedEmail = adminEmail.trim().toLowerCase();

        if (userRepository.findByEmailIgnoreCase(normalizedEmail).isEmpty()) {
            User admin = User.builder()
                    .firstName("System")
                    .lastName("Admin")
                    .email(normalizedEmail)
                    .password(passwordEncoder.encode(adminPassword.trim()))
                    .role(Role.SUPERADMIN)
                    .enabled(true)
                    .accountNonLocked(true)
                    .provider(AuthProvider.LOCAL)
                    .build();

            userRepository.save(admin);
            log.info(">>> Initial Administrator account initialized successfully for email: {}", normalizedEmail);
        } else {
            log.info(">>> Administrator account ({}) already exists. Skipping initialization.", normalizedEmail);
        }
    }
}