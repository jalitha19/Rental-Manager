package com.rental.controller;

import com.rental.dto.CreateFirstAdminRequest;
import com.rental.entity.User;
import com.rental.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class FirstAdminController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/first-admin")
    @ResponseStatus(HttpStatus.CREATED)
    public void createFirstAdmin(@Valid @RequestBody CreateFirstAdminRequest request) {
        if (userRepository.count() > 0) {
            throw new IllegalStateException("The first admin account has already been created.");
        }

        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalStateException("Username is already taken.");
        }

        User admin = User.builder()
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();

        userRepository.save(admin);
    }
}
