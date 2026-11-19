package com.example.hospital_manager.service;

import com.example.hospital_manager.dto.AuthResponse;
import com.example.hospital_manager.dto.LoginRequest;
import com.example.hospital_manager.dto.RegisterRequest;
import com.example.hospital_manager.dto.UserResponse;
import com.example.hospital_manager.exception.ApiException;
import com.example.hospital_manager.model.AppUser;
import com.example.hospital_manager.model.Role;
import com.example.hospital_manager.repo.UserRepository;
import com.example.hospital_manager.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse login(@Valid LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        AppUser user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        String token = jwtService.generateToken(user.getUsername(), user.getRole());
        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpirationMs(),
                user.getUsername(),
                user.getFullName(),
                user.getRole()
        );
    }

    @Transactional
    public UserResponse register(@Valid RegisterRequest request) {
        // Enforce role-based registration rules:
        // NURSE and RECEPTIONIST accounts require an authenticated ADMIN
        if (request.role() == Role.NURSE || request.role() == Role.RECEPTIONIST) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = auth != null && auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (!isAdmin) {
                throw new ApiException("Only administrators can register a " + request.role(), HttpStatus.FORBIDDEN);
            }
        }

        if (userRepository.existsByUsername(request.username())) {
            throw new ApiException("Username already exists", HttpStatus.CONFLICT);
        }

        String fullName = request.fullName();
        if (fullName == null || fullName.isBlank()) {
            fullName = request.username();
        }

        AppUser saved = userRepository.save(AppUser.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .fullName(fullName)
                .role(request.role())
                .build());

        return new UserResponse(saved.getId(), saved.getUsername(), saved.getFullName(), saved.getRole());
    }

    /**
     * Admin-only endpoint helper: creates NURSE or RECEPTIONIST staff accounts.
     */
    @Transactional
    public UserResponse registerStaff(@Valid RegisterRequest request) {
        return register(request);
    }

    public UserResponse getCurrentUser(String username) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        return new UserResponse(user.getId(), user.getUsername(), user.getFullName(), user.getRole());
    }
}
