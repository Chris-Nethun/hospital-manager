package com.example.hospital_manager.controller;

import com.example.hospital_manager.dto.DoctorCreateRequest;
import com.example.hospital_manager.dto.RegisterRequest;
import com.example.hospital_manager.dto.UserResponse;
import com.example.hospital_manager.model.AppUser;
import com.example.hospital_manager.model.Role;
import com.example.hospital_manager.repo.UserRepository;
import com.example.hospital_manager.service.AuthService;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class StaffController {

    private final UserRepository userRepository;
    private final AuthService authService;

    public StaffController(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    /**
     * List all users — ADMIN only.
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AppUser> getUsers() {
        return userRepository.findAll();
    }

    /**
     * List all doctors — ADMIN, RECEPTIONIST, PATIENT can view the doctor list to book.
     */
    @GetMapping("/doctors")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','NURSE','RECEPTIONIST','PATIENT')")
    public List<AppUser> getDoctors() {
        return userRepository.findByRole(Role.DOCTOR);
    }

    /**
     * Create a new doctor account — ADMIN only.
     */
    @PostMapping("/admin/doctors")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse createDoctor(@Valid @RequestBody DoctorCreateRequest request) {
        return authService.register(new RegisterRequest(request.username(), request.password(), request.fullName(), Role.DOCTOR));
    }

    /**
     * Create a new staff account with NURSE or RECEPTIONIST role — ADMIN only.
     * Demonstrates that a PATIENT token (even a tampered one claiming ADMIN) cannot reach this.
     */
    @PostMapping("/admin/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse createStaff(@Valid @RequestBody RegisterRequest request) {
        if (request.role() == Role.ADMIN || request.role() == Role.DOCTOR || request.role() == Role.PATIENT) {
            throw new com.example.hospital_manager.exception.ApiException(
                    "Use /api/auth/register for ADMIN/DOCTOR/PATIENT roles, or /api/admin/doctors for DOCTOR",
                    org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        // NURSE or RECEPTIONIST — created by admin only
        return authService.registerStaff(request);
    }

    /**
     * Delete a user account by ID — ADMIN only.
     */
    @DeleteMapping("/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public org.springframework.http.ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new com.example.hospital_manager.exception.ApiException(
                        "User not found", org.springframework.http.HttpStatus.NOT_FOUND));
        userRepository.delete(user);
        return org.springframework.http.ResponseEntity.noContent().build();
    }

    /**
     * Delete a user account by username — ADMIN only.
     */
    @DeleteMapping("/admin/users/by-username/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public org.springframework.http.ResponseEntity<Void> deleteUserByUsername(@PathVariable String username) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.example.hospital_manager.exception.ApiException(
                        "User not found: " + username, org.springframework.http.HttpStatus.NOT_FOUND));
        userRepository.delete(user);
        return org.springframework.http.ResponseEntity.noContent().build();
    }
}
