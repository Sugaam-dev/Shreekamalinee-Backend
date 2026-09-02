package com.pmrgsolution.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.pmrgsolution.Constant.AuthProvider;
import com.pmrgsolution.Constant.Role;
import com.pmrgsolution.features.auth.entity.User;
import com.pmrgsolution.features.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.admin.email:admin@shreekamalinee.com}")
    private String adminEmail;

    @Value("${app.admin.password:Admin@ShreeKamalinee2026}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        // ---- 1. CLEANUP LEGACY DB CONSTRAINTS ON CATEGORY SLUG (RUNS ONLY IF LEGACY CONSTRAINTS EXIST) ----
        try {
            Integer legacyCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.table_constraints " +
                    "WHERE table_name = 'categories' AND constraint_type = 'UNIQUE' " +
                    "  AND constraint_name != 'uq_category_slug_parent' AND constraint_name != 'categories_pkey'",
                    Integer.class);

            if (legacyCount != null && legacyCount > 0) {
                jdbcTemplate.execute("DO $$ DECLARE r RECORD; BEGIN " +
                        "FOR r IN (SELECT constraint_name FROM information_schema.table_constraints " +
                        "          WHERE table_name = 'categories' AND constraint_type = 'UNIQUE' " +
                        "            AND constraint_name != 'uq_category_slug_parent' AND constraint_name != 'categories_pkey') LOOP " +
                        "  EXECUTE 'ALTER TABLE categories DROP CONSTRAINT IF EXISTS ' || quote_ident(r.constraint_name); " +
                        "END LOOP; " +
                        "END $$;");
                log.info("Migration: Successfully removed {} legacy category unique constraint(s)", legacyCount);
            }
        } catch (Exception e) {
            log.debug("Category constraint migration check: {}", e.getMessage());
        }

        // ---- 2. INITIALIZE SUPERADMIN ACCOUNT ----
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