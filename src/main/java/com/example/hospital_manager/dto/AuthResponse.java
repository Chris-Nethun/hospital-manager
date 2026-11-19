package com.example.hospital_manager.dto;

import com.example.hospital_manager.model.Role;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMs,
        String username,
        String fullName,
        Role role
) {
    public String type() {
        return tokenType;
    }

    public long expiry() {
        return expiresInMs;
    }
}
