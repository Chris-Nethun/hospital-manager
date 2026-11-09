package com.example.hospital_manager.repo;

import com.example.hospital_manager.model.MedicalRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {

    @Query("SELECT mr FROM MedicalRecord mr JOIN FETCH mr.patient JOIN FETCH mr.doctor WHERE mr.patient.id = :patientId ORDER BY mr.createdAt DESC")
    List<MedicalRecord> findByPatientIdWithDetails(@Param("patientId") Long patientId);

    boolean existsByPatientId(Long patientId);
}
