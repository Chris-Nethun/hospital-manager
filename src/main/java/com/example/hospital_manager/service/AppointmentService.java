package com.example.hospital_manager.service;

import com.example.hospital_manager.dto.AppointmentRequest;
import com.example.hospital_manager.dto.AppointmentResponse;
import com.example.hospital_manager.dto.StatusRequest;
import com.example.hospital_manager.exception.ApiException;
import com.example.hospital_manager.model.AppUser;
import com.example.hospital_manager.model.Appointment;
import com.example.hospital_manager.model.AppointmentStatus;
import com.example.hospital_manager.model.Patient;
import com.example.hospital_manager.model.Role;
import com.example.hospital_manager.repo.AppointmentRepository;
import com.example.hospital_manager.repo.PatientRepository;
import com.example.hospital_manager.repo.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              PatientRepository patientRepository,
                              UserRepository userRepository) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
    }

    public List<AppointmentResponse> listAppointments(Long doctorId) {
        List<Appointment> appointments;
        if (doctorId != null) {
            AppUser doctor = userRepository.findById(doctorId)
                    .orElseThrow(() -> new ApiException("Doctor not found", HttpStatus.NOT_FOUND));
            appointments = appointmentRepository.findByDoctorWithDetails(doctor);
        } else {
            appointments = appointmentRepository.findAllWithDetails();
        }

        return appointments.stream().map(this::toResponse).toList();
    }

    public AppointmentResponse createAppointment(AppointmentRequest request) {
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ApiException("Patient not found", HttpStatus.NOT_FOUND));
        AppUser doctor = userRepository.findById(request.doctorId())
                .orElseThrow(() -> new ApiException("Doctor not found", HttpStatus.NOT_FOUND));

        if (!doctor.getRole().equals(Role.DOCTOR)) {
            throw new ApiException("Only doctors can be assigned to appointments", HttpStatus.BAD_REQUEST);
        }

        if (request.appointmentDateTime().isBefore(LocalDateTime.now())) {
            throw new ApiException("Appointment date must be in the future", HttpStatus.BAD_REQUEST);
        }

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .appointmentDateTime(request.appointmentDateTime())
                .status(AppointmentStatus.SCHEDULED)
                .notes(request.notes())
                .build();

        return toResponse(appointmentRepository.save(appointment));
    }

    public AppointmentResponse updateStatus(Long appointmentId, StatusRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ApiException("Appointment not found", HttpStatus.NOT_FOUND));
        appointment.setStatus(request.status());
        return toResponse(appointmentRepository.save(appointment));
    }

    private AppointmentResponse toResponse(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getPatient().getId(),
                appointment.getPatient().getFirstName() + " " + appointment.getPatient().getLastName(),
                appointment.getDoctor().getId(),
                appointment.getDoctor().getUsername(),
                appointment.getAppointmentDateTime(),
                appointment.getStatus(),
                appointment.getNotes()
        );
    }
}
