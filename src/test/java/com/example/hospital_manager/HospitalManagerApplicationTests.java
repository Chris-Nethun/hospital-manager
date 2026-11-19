package com.example.hospital_manager;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class HospitalManagerApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	/**
	 * Patient registration:
	 * No patientId required in registration body!
	 */
	@Test
	void registersPatientSuccessfullyWithoutPatientId() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "patient-sample",
								  "password": "password123",
								  "fullName": "Test Patient",
								  "role": "PATIENT"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("patient-sample"))
				.andExpect(jsonPath("$.fullName").value("Test Patient"))
				.andExpect(jsonPath("$.role").value("PATIENT"));
	}
}
