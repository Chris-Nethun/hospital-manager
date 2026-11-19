package com.example.hospital_manager.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.hospital_manager.dto.AppointmentRequest;
import com.example.hospital_manager.dto.AppointmentResponse;
import com.example.hospital_manager.dto.PatientAppointmentRequest;
import com.example.hospital_manager.dto.StatusRequest;
import com.example.hospital_manager.exception.ApiException;
import com.example.hospital_manager.model.*;
import com.example.hospital_manager.repo.AppointmentRepository;
import com.example.hospital_manager.repo.PatientRepository;
import com.example.hospital_manager.repo.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private AppointmentService appointmentService;

    // ─────────────────────────── helpers ──────────────────────────────

    private Patient samplePatient(Long id) {
        return Patient.builder().id(id)
                .firstName("Alice").lastName("Johnson")
                .email("alice@example.com").phone("+1234567890")
                .dateOfBirth(java.time.LocalDate.of(1988, 5, 12))
                .gender("Female").address("123 Main St")
                .emergencyContact("John").build();
    }

    private AppUser sampleDoctor(Long id) {
        return AppUser.builder().id(id).username("doctor1").role(Role.DOCTOR).build();
    }

    // ─────────────────────────── createAppointment ────────────────────

    @Test
    void createAppointment_savesAndReturnsResponse() {
        Patient patient = samplePatient(1L);
        AppUser doctor = sampleDoctor(2L);
        LocalDateTime futureDate = LocalDateTime.now().plusDays(1);

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(userRepository.findById(2L)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> {
            Appointment a = inv.getArgument(0);
            a = Appointment.builder()
                    .id(10L).patient(a.getPatient()).doctor(a.getDoctor())
                    .appointmentDateTime(a.getAppointmentDateTime())
                    .status(AppointmentStatus.SCHEDULED).notes(a.getNotes()).build();
            return a;
        });

        AppointmentResponse response = appointmentService.createAppointment(
                new AppointmentRequest(1L, 2L, futureDate, "Annual checkup"));

        assertEquals(10L, response.id());
        assertEquals(1L, response.patientId());
        assertEquals(2L, response.doctorId());
        assertEquals(AppointmentStatus.SCHEDULED, response.status());
        assertEquals("Annual checkup", response.notes());
    }

    @Test
    void createAppointment_rejectsNonDoctorUser() {
        Patient patient = samplePatient(1L);
        AppUser nurse = AppUser.builder().id(2L).username("nurse1").role(Role.NURSE).build();

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(userRepository.findById(2L)).thenReturn(Optional.of(nurse));

        ApiException ex = assertThrows(ApiException.class, () ->
                appointmentService.createAppointment(
                        new AppointmentRequest(1L, 2L, LocalDateTime.now().plusDays(1), null)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void createAppointment_rejectsPastDate() {
        Patient patient = samplePatient(1L);
        AppUser doctor = sampleDoctor(2L);

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(userRepository.findById(2L)).thenReturn(Optional.of(doctor));

        ApiException ex = assertThrows(ApiException.class, () ->
                appointmentService.createAppointment(
                        new AppointmentRequest(1L, 2L, LocalDateTime.now().minusDays(1), null)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void createAppointment_rejectsUnknownPatient() {
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () ->
                appointmentService.createAppointment(
                        new AppointmentRequest(99L, 2L, LocalDateTime.now().plusDays(1), null)));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    // ─────────────────────────── updateStatus ─────────────────────────

    @Test
    void updateStatus_changesAppointmentStatus() {
        Patient patient = samplePatient(1L);
        AppUser doctor = sampleDoctor(2L);
        Appointment appointment = Appointment.builder()
                .id(5L).patient(patient).doctor(doctor)
                .appointmentDateTime(LocalDateTime.now().plusDays(1))
                .status(AppointmentStatus.SCHEDULED).build();

        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AppointmentResponse response = appointmentService.updateStatus(5L,
                new StatusRequest(AppointmentStatus.COMPLETED));

        assertEquals(AppointmentStatus.COMPLETED, response.status());
    }

    @Test
    void updateStatus_throwsWhenAppointmentNotFound() {
        when(appointmentRepository.findById(999L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () ->
                appointmentService.updateStatus(999L, new StatusRequest(AppointmentStatus.CANCELLED)));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    // ─────────────────────────── listDoctorAppointments ───────────────

    @Test
    void listDoctorAppointments_doctorCanOnlyViewOwn() {
        AppUser doctor = sampleDoctor(2L);
        when(userRepository.findByUsername("doctor1")).thenReturn(Optional.of(doctor));
        when(appointmentRepository.findByDoctorWithDetails(doctor)).thenReturn(List.of());

        List<AppointmentResponse> result = appointmentService.listDoctorAppointments("doctor1", null);
        assertTrue(result.isEmpty());
        verify(appointmentRepository).findByDoctorWithDetails(doctor);
    }

    @Test
    void listDoctorAppointments_doctorCannotViewOtherDoctorAppointments() {
        AppUser doctor = sampleDoctor(2L);
        when(userRepository.findByUsername("doctor1")).thenReturn(Optional.of(doctor));

        // Requesting doctorId=5 (different from own id=2)
        ApiException ex = assertThrows(ApiException.class, () ->
                appointmentService.listDoctorAppointments("doctor1", 5L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void listDoctorAppointments_adminCanViewAny() {
        AppUser admin = AppUser.builder().id(1L).username("admin").role(Role.ADMIN).build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(appointmentRepository.findAllWithDetails()).thenReturn(List.of());

        List<AppointmentResponse> result = appointmentService.listDoctorAppointments("admin", null);
        assertTrue(result.isEmpty());
        verify(appointmentRepository).findAllWithDetails();
    }

    // ─────────────────────────── createAppointmentForPatient ──────────

    @Test
    void createAppointmentForPatient_linksToPatientProfile() {
        Patient patient = samplePatient(1L);
        AppUser patientUser = AppUser.builder().id(3L).username("patient1")
                .role(Role.PATIENT).patientProfile(patient).build();
        AppUser doctor = sampleDoctor(2L);
        LocalDateTime future = LocalDateTime.now().plusDays(2);

        when(userRepository.findByUsername("patient1")).thenReturn(Optional.of(patientUser));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(userRepository.findById(2L)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> {
            Appointment a = inv.getArgument(0);
            return Appointment.builder().id(20L).patient(a.getPatient()).doctor(a.getDoctor())
                    .appointmentDateTime(a.getAppointmentDateTime())
                    .status(AppointmentStatus.SCHEDULED).notes(a.getNotes()).build();
        });

        AppointmentResponse response = appointmentService.createAppointmentForPatient(
                "patient1", new PatientAppointmentRequest(2L, future, "checkup"));

        assertEquals(1L, response.patientId());
        assertEquals(20L, response.id());
    }

    @Test
    void createAppointmentForPatient_failsWhenUserNotPatientRole() {
        AppUser doctorUser = AppUser.builder().id(3L).username("doctor1")
                .role(Role.DOCTOR).build();

        when(userRepository.findByUsername("doctor1")).thenReturn(Optional.of(doctorUser));

        ApiException ex = assertThrows(ApiException.class, () ->
                appointmentService.createAppointmentForPatient("doctor1",
                        new PatientAppointmentRequest(2L, LocalDateTime.now().plusDays(1), null)));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }
}
