package com.example.hospital_manager.service;

import com.example.hospital_manager.dto.MedicalRecordRequest;
import com.example.hospital_manager.dto.MedicalRecordResponse;
import com.example.hospital_manager.exception.ApiException;
import com.example.hospital_manager.model.AppUser;
import com.example.hospital_manager.model.MedicalRecord;
import com.example.hospital_manager.model.Patient;
import com.example.hospital_manager.model.Role;
import com.example.hospital_manager.repo.MedicalRecordRepository;
import com.example.hospital_manager.repo.PatientRepository;
import com.example.hospital_manager.repo.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;

    public MedicalRecordService(MedicalRecordRepository medicalRecordRepository,
                               PatientRepository patientRepository,
                               UserRepository userRepository) {
        this.medicalRecordRepository = medicalRecordRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
    }

    public List<MedicalRecordResponse> getRecordsForPatient(Long patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new ApiException("Patient not found", HttpStatus.NOT_FOUND);
        }
        return medicalRecordRepository.findByPatientIdWithDetails(patientId).stream().map(this::toResponse).toList();
    }

    public MedicalRecordResponse createRecord(Long patientId, String doctorUsername, MedicalRecordRequest request) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ApiException("Patient not found", HttpStatus.NOT_FOUND));
        AppUser doctor = userRepository.findByUsername(doctorUsername)
                .orElseThrow(() -> new ApiException("Doctor not found", HttpStatus.NOT_FOUND));

        if (!doctor.getRole().equals(Role.DOCTOR)) {
            throw new ApiException("Only doctors can write medical records", HttpStatus.FORBIDDEN);
        }

        MedicalRecord record = MedicalRecord.builder()
                .patient(patient)
                .doctor(doctor)
                .diagnosis(request.diagnosis())
                .prescription(request.prescription())
                .build();

        return toResponse(medicalRecordRepository.save(record));
    }

    private MedicalRecordResponse toResponse(MedicalRecord record) {
        return new MedicalRecordResponse(
                record.getId(),
                record.getPatient().getId(),
                record.getPatient().getFirstName() + " " + record.getPatient().getLastName(),
                record.getDoctor().getId(),
                record.getDoctor().getUsername(),
                record.getDiagnosis(),
                record.getPrescription(),
                record.getCreatedAt()
        );
    }
}
