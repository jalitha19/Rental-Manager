package com.rental.controller;

import com.rental.dto.ChangePasswordRequest;
import com.rental.dto.LoginRequest;
import com.rental.dto.LoginResponse;
import com.rental.dto.UserSummaryResponse;
import com.rental.entity.User;
import com.rental.exception.ResourceNotFoundException;
import com.rental.repository.UserRepository;
import com.rental.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * Handles login for the single administrator account.
 *
 * There is no registration endpoint by design -- the admin account is
 * seeded once at startup (see DataInitializer). "Logout" for a stateless
 * JWT API simply means the frontend discards the token; there is nothing
 * to invalidate server-side.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (org.springframework.security.core.AuthenticationException ex) {
            // Deliberately vague -- never reveal whether the username or password was wrong.
            throw new BadCredentialsException("Invalid username or password");
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);

        return new LoginResponse(token, userDetails.getUsername(), jwtExpirationMs);
    }

    @GetMapping("/me")
    public UserSummaryResponse me(Authentication authentication) {
        return new UserSummaryResponse(authentication.getName());
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request, Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }
}
