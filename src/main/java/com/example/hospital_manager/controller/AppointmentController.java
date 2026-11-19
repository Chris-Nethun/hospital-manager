package com.example.hospital_manager.controller;

import com.example.hospital_manager.dto.AppointmentRequest;
import com.example.hospital_manager.dto.AppointmentResponse;
import com.example.hospital_manager.dto.PatientAppointmentRequest;
import com.example.hospital_manager.dto.StatusRequest;
import com.example.hospital_manager.service.AppointmentService;
import jakarta.validation.Valid;
import java.util.List;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping("/appointments")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','NURSE','RECEPTIONIST')")
    public List<AppointmentResponse> getAppointments(@RequestParam(required = false) Long doctorId) {
        return appointmentService.listAppointments(doctorId);
    }

    @PostMapping("/appointments")
    @PreAuthorize("hasAnyRole('DOCTOR','RECEPTIONIST')")
    public ResponseEntity<AppointmentResponse> createAppointment(@Valid @RequestBody AppointmentRequest request) {
        return ResponseEntity.ok(appointmentService.createAppointment(request));
    }

    @GetMapping("/doctors/appointments")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    public List<AppointmentResponse> getDoctorAppointments(
            @RequestParam(required = false) Long doctorId, Principal principal) {
        return appointmentService.listDoctorAppointments(principal.getName(), doctorId);
    }

    @PostMapping("/patients/appointments")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<AppointmentResponse> bookPatientAppointment(
            @Valid @RequestBody PatientAppointmentRequest request, Principal principal) {
        return ResponseEntity.ok(appointmentService.createAppointmentForPatient(principal.getName(), request));
    }

    @PutMapping("/appointments/{id}/status")
    @PreAuthorize("hasAnyRole('DOCTOR','RECEPTIONIST')")
    public ResponseEntity<AppointmentResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        return ResponseEntity.ok(appointmentService.updateStatus(id, request));
    }
}
