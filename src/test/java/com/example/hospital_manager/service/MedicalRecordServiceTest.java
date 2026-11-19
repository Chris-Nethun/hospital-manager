package com.example.hospital_manager.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.hospital_manager.dto.MedicalRecordRequest;
import com.example.hospital_manager.dto.MedicalRecordResponse;
import com.example.hospital_manager.exception.ApiException;
import com.example.hospital_manager.model.*;
import com.example.hospital_manager.repo.MedicalRecordRepository;
import com.example.hospital_manager.repo.PatientRepository;
import com.example.hospital_manager.repo.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class MedicalRecordServiceTest {

    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private MedicalRecordService medicalRecordService;

    private Patient samplePatient(Long id) {
        return Patient.builder().id(id)
                .firstName("Alice").lastName("Johnson")
                .email("alice@example.com").phone("+1234567890")
                .dateOfBirth(LocalDate.of(1988, 5, 12))
                .gender("Female").address("123 Main St")
                .emergencyContact("John").build();
    }

    private AppUser sampleDoctor(Long id) {
        return AppUser.builder().id(id).username("doctor1").role(Role.DOCTOR).build();
    }

    // ──────────────────────── createRecord ────────────────────────────

    @Test
    void createRecord_savesAndReturnsResponse() {
        Patient patient = samplePatient(1L);
        AppUser doctor = sampleDoctor(2L);
        MedicalRecordRequest request = new MedicalRecordRequest("Flu", "Rest and fluids");

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(userRepository.findByUsername("doctor1")).thenReturn(Optional.of(doctor));
        when(medicalRecordRepository.save(any(MedicalRecord.class))).thenAnswer(inv -> {
            MedicalRecord mr = inv.getArgument(0);
            return MedicalRecord.builder()
                    .id(10L).patient(mr.getPatient()).doctor(mr.getDoctor())
                    .diagnosis(mr.getDiagnosis()).prescription(mr.getPrescription())
                    .createdAt(LocalDateTime.now()).build();
        });

        MedicalRecordResponse response = medicalRecordService.createRecord(1L, "doctor1", request);

        assertEquals(10L, response.id());
        assertEquals(1L, response.patientId());
        assertEquals("Alice Johnson", response.patientName());
        assertEquals("Flu", response.diagnosis());
        assertEquals("Rest and fluids", response.prescription());
    }

    @Test
    void createRecord_throwsForbidden_whenUserIsNotDoctor() {
        Patient patient = samplePatient(1L);
        AppUser nurse = AppUser.builder().id(2L).username("nurse1").role(Role.NURSE).build();

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(userRepository.findByUsername("nurse1")).thenReturn(Optional.of(nurse));

        ApiException ex = assertThrows(ApiException.class, () ->
                medicalRecordService.createRecord(1L, "nurse1",
                        new MedicalRecordRequest("Flu", "Rest")));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void createRecord_throwsNotFound_whenPatientMissing() {
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () ->
                medicalRecordService.createRecord(99L, "doctor1",
                        new MedicalRecordRequest("Flu", "Rest")));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    // ──────────────────────── getRecordsForPatient ────────────────────

    @Test
    void getRecordsForPatient_returnsEmptyList_whenNoRecords() {
        when(patientRepository.existsById(1L)).thenReturn(true);
        when(medicalRecordRepository.findByPatientIdWithDetails(1L)).thenReturn(List.of());

        List<MedicalRecordResponse> result = medicalRecordService.getRecordsForPatient(1L);
        assertTrue(result.isEmpty());
    }

    @Test
    void getRecordsForPatient_throwsNotFound_whenPatientMissing() {
        when(patientRepository.existsById(99L)).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class, () ->
                medicalRecordService.getRecordsForPatient(99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
