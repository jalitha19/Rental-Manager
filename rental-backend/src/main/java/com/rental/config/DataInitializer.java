package com.rental.config;

import com.rental.entity.User;
import com.rental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds the one admin account the very first time the app starts against an
 * empty users table. There is no registration endpoint by design (single
 * admin, no self-service signup) -- this is the only way an account gets
 * created.
 *
 * Set ADMIN_USERNAME and ADMIN_INITIAL_PASSWORD as environment variables
 * before first startup. If they're left at the defaults, a warning is
 * logged -- change the password via Settings immediately after first login.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String defaultAdminUsername;

    @Value("${app.admin.initial-password:changeme}")
    private String defaultAdminPassword;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return; // admin account already exists -- nothing to do
        }

        User admin = User.builder()
                .username(defaultAdminUsername)
                .passwordHash(passwordEncoder.encode(defaultAdminPassword))
                .build();

        userRepository.save(admin);

        log.warn("Created initial admin account '{}'. If ADMIN_INITIAL_PASSWORD was not set via " +
                "environment variable, a default password is in use -- log in and change it immediately.",
                defaultAdminUsername);
    }
}
