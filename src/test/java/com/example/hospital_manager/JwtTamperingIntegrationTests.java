package com.example.hospital_manager;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.hospital_manager.model.Role;
import com.example.hospital_manager.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests that verify the JWT tampering-detection requirement
 * described in Assignment 02 — Task 2.
 *
 * <p>A PATIENT token whose payload is tampered to show ROLE_ADMIN must be
 * rejected with 401 Unauthorized because the HMAC-SHA256 signature no longer
 * matches the altered payload.
 */
@SpringBootTest
@AutoConfigureMockMvc
class JwtTamperingIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;

    // ─────────────── helpers ──────────────────────────────────────────

    /** Build a valid JWT for the given user/role, then swap the payload for a tampered one. */
    private String buildTamperedAdminToken(String username) {
        String realToken = jwtService.generateToken(username, Role.PATIENT);
        String[] parts = realToken.split("\\.");

        // Payload claims ADMIN role — same username, different role
        String tamperedPayload = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(
                        ("{\"sub\":\"" + username + "\",\"role\":\"ADMIN\","
                                + "\"iat\":1793941358,\"exp\":9999999999}").getBytes());

        // Header and signature stay from the original token
        return parts[0] + "." + tamperedPayload + "." + parts[2];
    }

    // ─────────────── tests ────────────────────────────────────────────

    /**
     * Task 2 — Step 4:
     * POST /api/admin/staff with a tampered PATIENT→ADMIN token must be rejected (401).
     */
    @Test
    void tamperedToken_isRejectedWhenCreatingStaff() throws Exception {
        String tampered = buildTamperedAdminToken("patient-tamper-test-staff");

        mockMvc.perform(post("/api/admin/staff")
                        .header("Authorization", "Bearer " + tampered)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "badnurse",
                                  "password": "badnurse1",
                                  "fullName": "Bad Nurse",
                                  "role": "NURSE"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Task 2 — Step 4 (exact endpoint from PDF screenshot):
     * POST /api/auth/register with a tampered token attempting to add a NURSE must be rejected (401).
     */
    @Test
    void tamperedToken_isRejectedAtAuthRegisterWhenCreatingNurse() throws Exception {
        String tampered = buildTamperedAdminToken("patient_tamper_user");

        mockMvc.perform(post("/api/auth/register")
                        .header("Authorization", "Bearer " + tampered)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "TEST",
                                  "password": "TEST",
                                  "fullName": "TEST NURSE",
                                  "role": "NURSE"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Task 2 — Step 5:
     * GET /api/patients with a tampered PATIENT→ADMIN token must be rejected (401).
     */
    @Test
    void tamperedToken_isRejectedWhenViewingPatients() throws Exception {
        String tampered = buildTamperedAdminToken("patient-tamper-test-patients");

        mockMvc.perform(get("/api/patients")
                        .header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Sanity check: a VALID admin token CAN reach admin-only endpoints.
     */
    @Test
    void validAdminToken_allowsAccessToAdminPatients() throws Exception {
        // Log in as admin to get a real token
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "admin", "password": "admin123"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Extract token from JSON response
        String token = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(loginResponse).get("token").asText();

        mockMvc.perform(get("/api/admin/patients")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    /**
     * Sanity check: a request with no token is rejected (401).
     */
    @Test
    void noToken_isRejectedForProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isUnauthorized());
    }
}
