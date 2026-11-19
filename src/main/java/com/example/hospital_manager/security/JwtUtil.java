package com.example.hospital_manager.security;

/**
 * Utility constants and helpers for JWT handling.
 */
public final class JwtUtil {

    public static final String BEARER_PREFIX = "Bearer ";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String ROLE_CLAIM = "role";

    private JwtUtil() {
        // utility class — no instances
    }

    /**
     * Extracts the raw token from the Authorization header value.
     * Returns {@code null} if the header is missing or not a Bearer token.
     */
    public static String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            return authorizationHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}

