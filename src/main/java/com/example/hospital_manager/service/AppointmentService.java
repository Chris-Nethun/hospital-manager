package com.example.hospital_manager.service;

import com.example.hospital_manager.dto.AppointmentRequest;
import com.example.hospital_manager.dto.AppointmentResponse;
import com.example.hospital_manager.dto.PatientAppointmentRequest;
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
import org.springframework.transaction.annotation.Transactional;

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

    public List<AppointmentResponse> listDoctorAppointments(String username, Long requestedDoctorId) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        if (user.getRole() == Role.ADMIN) {
            return listAppointments(requestedDoctorId);
        }
        if (user.getRole() != Role.DOCTOR) {
            throw new ApiException("Only doctors and admins can view doctor appointments", HttpStatus.FORBIDDEN);
        }
        if (requestedDoctorId != null && !requestedDoctorId.equals(user.getId())) {
            throw new ApiException("Doctors can only view their own appointments", HttpStatus.FORBIDDEN);
        }
        return appointmentRepository.findByDoctorWithDetails(user).stream().map(this::toResponse).toList();
    }

    public AppointmentResponse createAppointment(AppointmentRequest request) {
        return createAppointment(request.patientId(), request.doctorId(), request.appointmentDateTime(), request.notes());
    }

    @Transactional
    public AppointmentResponse createAppointmentForPatient(String username, PatientAppointmentRequest request) {
        AppUser patientUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        if (patientUser.getRole() != Role.PATIENT) {
            throw new ApiException("Only patients can book patient appointments", HttpStatus.FORBIDDEN);
        }

        Patient patient = patientUser.getPatientProfile();
        if (patient == null) {
            String fName = patientUser.getFullName() != null && !patientUser.getFullName().isBlank()
                    ? patientUser.getFullName() : patientUser.getUsername();
            String lName = "Patient";
            if (fName.contains(" ")) {
                int idx = fName.lastIndexOf(" ");
                lName = fName.substring(idx + 1);
                fName = fName.substring(0, idx);
            }
            String email = patientUser.getUsername().replaceAll("[^a-zA-Z0-9]", "") + "@hospital.com";
            patient = patientRepository.save(Patient.builder()
                    .firstName(fName)
                    .lastName(lName)
                    .email(email)
                    .phone("+1000000000")
                    .dateOfBirth(java.time.LocalDate.of(2000, 1, 1))
                    .gender("Other")
                    .address("Hospital Client")
                    .emergencyContact("N/A")
                    .build());
            patientUser.setPatientProfile(patient);
            userRepository.save(patientUser);
        }

        return createAppointment(patient.getId(), request.doctorId(),
                request.appointmentDateTime(), request.notes());
    }

    private AppointmentResponse createAppointment(Long patientId, Long doctorId,
                                                  LocalDateTime appointmentDateTime, String notes) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ApiException("Patient not found", HttpStatus.NOT_FOUND));
        AppUser doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> new ApiException("Doctor not found", HttpStatus.NOT_FOUND));

        if (!doctor.getRole().equals(Role.DOCTOR)) {
            throw new ApiException("Only doctors can be assigned to appointments", HttpStatus.BAD_REQUEST);
        }

        if (appointmentDateTime.isBefore(LocalDateTime.now())) {
            throw new ApiException("Appointment date must be in the future", HttpStatus.BAD_REQUEST);
        }

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .appointmentDateTime(appointmentDateTime)
                .status(AppointmentStatus.SCHEDULED)
                .notes(notes)
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
