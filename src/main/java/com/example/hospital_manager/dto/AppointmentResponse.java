package com.example.hospital_manager.dto;

import com.example.hospital_manager.model.AppointmentStatus;
import java.time.LocalDateTime;

public record AppointmentResponse(
        Long id,
        Long patientId,
        String patientName,
        Long doctorId,
        String doctorName,
        LocalDateTime appointmentDateTime,
        AppointmentStatus status,
        String notes
) {
}
