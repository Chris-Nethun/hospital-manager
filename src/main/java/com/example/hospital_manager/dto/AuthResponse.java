package com.example.hospital_manager.dto;

import com.example.hospital_manager.model.Role;

public record AuthResponse(
        String token,
        String type,
        long expiry,
        Role role
) {
}
