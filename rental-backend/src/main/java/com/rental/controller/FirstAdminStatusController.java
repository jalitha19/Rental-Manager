package com.rental.controller;

import com.rental.dto.FirstAdminStatusResponse;
import com.rental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class FirstAdminStatusController {

    private final UserRepository userRepository;

    @GetMapping("/first-admin/status")
    public FirstAdminStatusResponse status() {
        return new FirstAdminStatusResponse(userRepository.count() == 0);
    }
}
