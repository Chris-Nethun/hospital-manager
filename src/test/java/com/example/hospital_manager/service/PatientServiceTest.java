package com.example.hospital_manager.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.hospital_manager.exception.ApiException;
import com.example.hospital_manager.model.Patient;
import com.example.hospital_manager.repo.AppointmentRepository;
import com.example.hospital_manager.repo.MedicalRecordRepository;
import com.example.hospital_manager.repo.PatientRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock private PatientRepository patientRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private MedicalRecordRepository medicalRecordRepository;
    @InjectMocks private PatientService patientService;

    private Patient buildPatient(Long id) {
        return Patient.builder().id(id)
                .firstName("Alice").lastName("Johnson")
                .email("alice@example.com").phone("+1234567890")
                .dateOfBirth(LocalDate.of(1988, 5, 12))
                .gender("Female").address("123 Main St")
                .emergencyContact("John Johnson").build();
    }

    // ──────────────────────── getAllPatients ───────────────────────────

    @Test
    void getAllPatients_returnsAll_whenNameIsNull() {
        when(patientRepository.findAll()).thenReturn(List.of(buildPatient(1L), buildPatient(2L)));
        assertEquals(2, patientService.getAllPatients(null).size());
        verify(patientRepository).findAll();
    }

    @Test
    void getAllPatients_returnsAll_whenNameIsBlank() {
        when(patientRepository.findAll()).thenReturn(List.of(buildPatient(1L)));
        assertEquals(1, patientService.getAllPatients("  ").size());
        verify(patientRepository).findAll();
    }

    @Test
    void getAllPatients_searchesByName_whenNameProvided() {
        when(patientRepository.searchByName("alice")).thenReturn(List.of(buildPatient(1L)));
        assertEquals(1, patientService.getAllPatients("alice").size());
        verify(patientRepository).searchByName("alice");
    }

    // ──────────────────────── getPatientById ──────────────────────────

    @Test
    void getPatientById_returnsPatient_whenFound() {
        Patient patient = buildPatient(1L);
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        assertEquals(patient, patientService.getPatientById(1L));
    }

    @Test
    void getPatientById_throwsNotFound_whenMissing() {
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());
        ApiException ex = assertThrows(ApiException.class, () -> patientService.getPatientById(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    // ──────────────────────── createPatient ───────────────────────────

    @Test
    void createPatient_savesAndReturns() {
        Patient patient = buildPatient(null);
        Patient saved = buildPatient(1L);
        when(patientRepository.existsByEmail(patient.getEmail())).thenReturn(false);
        when(patientRepository.save(patient)).thenReturn(saved);

        Patient result = patientService.createPatient(patient);
        assertEquals(1L, result.getId());
    }

    @Test
    void createPatient_throwsConflict_whenEmailDuplicated() {
        Patient patient = buildPatient(null);
        when(patientRepository.existsByEmail(patient.getEmail())).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> patientService.createPatient(patient));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    // ──────────────────────── updatePatient ───────────────────────────

    @Test
    void updatePatient_updatesAndReturns() {
        Patient existing = buildPatient(1L);
        Patient update = Patient.builder().id(1L)
                .firstName("Alicia").lastName("Johnson")
                .email("alicia@example.com").phone("+1234567890")
                .dateOfBirth(LocalDate.of(1988, 5, 12))
                .gender("Female").address("789 Elm St")
                .emergencyContact("John Johnson").build();

        when(patientRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(patientRepository.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));

        Patient result = patientService.updatePatient(1L, update);
        assertEquals("Alicia", result.getFirstName());
        assertEquals("789 Elm St", result.getAddress());
    }

    // ──────────────────────── deletePatient ───────────────────────────

    @Test
    void deletePatient_deletesSuccessfully_whenNoRelatedData() {
        Patient patient = buildPatient(1L);
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(appointmentRepository.existsByPatientId(1L)).thenReturn(false);
        when(medicalRecordRepository.existsByPatientId(1L)).thenReturn(false);

        patientService.deletePatient(1L);
        verify(patientRepository).delete(patient);
    }

    @Test
    void deletePatient_throwsConflict_whenPatientHasAppointments() {
        Patient patient = buildPatient(1L);
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(appointmentRepository.existsByPatientId(1L)).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> patientService.deletePatient(1L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(patientRepository, never()).delete(any());
    }

    @Test
    void deletePatient_throwsConflict_whenPatientHasMedicalRecords() {
        Patient patient = buildPatient(1L);
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(appointmentRepository.existsByPatientId(1L)).thenReturn(false);
        when(medicalRecordRepository.existsByPatientId(1L)).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> patientService.deletePatient(1L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }
}
