package com.example.hospital_manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MedicalRecordRequest(
        @NotBlank(message = "Diagnosis is required")
        @Size(max = 1000, message = "Diagnosis must not exceed 1000 characters")
        String diagnosis,

        @NotBlank(message = "Prescription is required")
        @Size(max = 1000, message = "Prescription must not exceed 1000 characters")
        String prescription
) {
}
