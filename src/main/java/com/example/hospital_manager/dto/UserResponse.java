package com.example.hospital_manager.dto;

import com.example.hospital_manager.model.Role;

public record UserResponse(
        Long id,
        String username,
        Role role
) {
}
