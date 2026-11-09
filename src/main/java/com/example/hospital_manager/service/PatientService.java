package com.example.hospital_manager.service;

import com.example.hospital_manager.exception.ApiException;
import com.example.hospital_manager.model.Patient;
import com.example.hospital_manager.repo.AppointmentRepository;
import com.example.hospital_manager.repo.MedicalRecordRepository;
import com.example.hospital_manager.repo.PatientRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final MedicalRecordRepository medicalRecordRepository;

    public PatientService(PatientRepository patientRepository,
                         AppointmentRepository appointmentRepository,
                         MedicalRecordRepository medicalRecordRepository) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.medicalRecordRepository = medicalRecordRepository;
    }

    public List<Patient> getAllPatients(String name) {
        if (name == null || name.isBlank()) {
            return patientRepository.findAll();
        }
        return patientRepository.searchByName(name);
    }

    public Patient getPatientById(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ApiException("Patient not found", HttpStatus.NOT_FOUND));
    }

    public Patient createPatient(@Valid Patient patient) {
        if (patientRepository.existsByEmail(patient.getEmail())) {
            throw new ApiException("A patient with this email already exists", HttpStatus.CONFLICT);
        }
        return patientRepository.save(patient);
    }

    public Patient updatePatient(Long id, Patient updatedPatient) {
        Patient patient = getPatientById(id);
        patient.setFirstName(updatedPatient.getFirstName());
        patient.setLastName(updatedPatient.getLastName());
        patient.setEmail(updatedPatient.getEmail());
        patient.setPhone(updatedPatient.getPhone());
        patient.setDateOfBirth(updatedPatient.getDateOfBirth());
        patient.setGender(updatedPatient.getGender());
        patient.setAddress(updatedPatient.getAddress());
        patient.setEmergencyContact(updatedPatient.getEmergencyContact());
        return patientRepository.save(patient);
    }

    public void deletePatient(Long id) {
        Patient patient = getPatientById(id);
        if (appointmentRepository.existsByPatientId(id) || medicalRecordRepository.existsByPatientId(id)) {
            throw new ApiException("Cannot delete patient because associated appointment or medical record exists", HttpStatus.CONFLICT);
        }
        patientRepository.delete(patient);
    }
}
