package com.example.hospital_manager.security;

import static org.junit.jupiter.api.Assertions.*;

import com.example.hospital_manager.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for JwtService — validates token generation, extraction,
 * and the tampering-detection requirement from Assignment 02.
 */
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey",
                "change-me-very-long-secret-key-for-hospital-manager-1234567890");
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3600000L);
    }

    // ──────────────────────── token generation ────────────────────────

    @Test
    void generateToken_returnsNonNullToken() {
        String token = jwtService.generateToken("admin", Role.ADMIN);
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void generateToken_containsThreeJwtParts() {
        String token = jwtService.generateToken("doctor1", Role.DOCTOR);
        assertEquals(3, token.split("\\.").length,
                "JWT must have exactly three Base64url-encoded parts separated by dots");
    }

    // ──────────────────────── extraction ──────────────────────────────

    @Test
    void extractUsername_returnsCorrectSubject() {
        String token = jwtService.generateToken("alice", Role.PATIENT);
        assertEquals("alice", jwtService.extractUsername(token));
    }

    @Test
    void extractRole_returnsCorrectRole() {
        String token = jwtService.generateToken("doctor1", Role.DOCTOR);
        assertEquals(Role.DOCTOR, jwtService.extractRole(token));
    }

    @Test
    void extractRole_returnsAdminForAdminToken() {
        String token = jwtService.generateToken("admin", Role.ADMIN);
        assertEquals(Role.ADMIN, jwtService.extractRole(token));
    }

    // ──────────────────────── validation ──────────────────────────────

    @Test
    void isTokenValid_returnsTrueForFreshToken() {
        String token = jwtService.generateToken("nurse1", Role.NURSE);
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void isTokenValid_returnsFalseForExpiredToken() throws InterruptedException {
        // Generate a token that expires immediately
        ReflectionTestUtils.setField(jwtService, "expirationMs", 1L);
        String token = jwtService.generateToken("user", Role.PATIENT);
        Thread.sleep(10); // wait for expiry
        assertFalse(jwtService.isTokenValid(token));
    }

    // ──────────────────────── TAMPERING TESTS ─────────────────────────
    // These tests demonstrate the core security requirement from Assignment 02:
    // A token whose payload was modified MUST be rejected due to signature mismatch.

    @Test
    void isTokenValid_returnsFalseForCompletelyFakeToken() {
        // A plausible-looking JWT with a known-bad signature
        String fakeToken = "eyJhbGciOiJIUzI1NiJ9" +           // header
                           ".eyJzdWIiOiJoYWNrZXIiLCJyb2xlIjoiQURNSU4ifQ" +  // tampered payload
                           ".INVALIDSIGNATURE";                                 // wrong signature
        assertFalse(jwtService.isTokenValid(fakeToken),
                "A token with an invalid signature must be rejected");
    }

    @Test
    void isTokenValid_returnsFalseForTokenWithAlteredPayload() {
        // Generate a real PATIENT token, then alter the payload portion
        String realToken = jwtService.generateToken("patient_user", Role.PATIENT);
        String[] parts = realToken.split("\\.");

        // Replace the payload with one that claims ADMIN role
        String tamperedPayload = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"patient_user\",\"role\":\"ADMIN\",\"iat\":1793941358,\"exp\":9999999999}"
                        .getBytes());

        // Keep original header + signature; only payload is changed
        String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

        assertFalse(jwtService.isTokenValid(tamperedToken),
                "A token with a tampered payload (PATIENT→ADMIN) must be rejected due to signature mismatch");
    }

    @Test
    void extractRole_afterTamperingSignatureIsInvalidSoValidationFails() {
        // Build a valid PATIENT token
        String token = jwtService.generateToken("patient_user", Role.PATIENT);
        assertEquals(Role.PATIENT, jwtService.extractRole(token)); // sanity check

        // Tamper: change payload to claim ADMIN
        String[] parts = token.split("\\.");
        String tamperedPayload = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"patient_user\",\"role\":\"ADMIN\"}"
                        .getBytes());
        String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

        // The tampered token must NOT pass validation
        assertFalse(jwtService.isTokenValid(tamperedToken),
                "Signature must protect against role tampering");
    }

    // ──────────────────────── JwtUtil helpers ─────────────────────────

    @Test
    void jwtUtil_extractBearerToken_returnsToken() {
        String token = jwtService.generateToken("admin", Role.ADMIN);
        String header = "Bearer " + token;
        assertEquals(token, JwtUtil.extractBearerToken(header));
    }

    @Test
    void jwtUtil_extractBearerToken_returnsNullForNonBearerHeader() {
        assertNull(JwtUtil.extractBearerToken("Basic dXNlcjpwYXNz"));
        assertNull(JwtUtil.extractBearerToken(null));
    }
}
