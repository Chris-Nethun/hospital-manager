package com.example.hospital_manager.repo;

import com.example.hospital_manager.model.Appointment;
import com.example.hospital_manager.model.AppUser;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("SELECT a FROM Appointment a JOIN FETCH a.patient JOIN FETCH a.doctor ORDER BY a.appointmentDateTime")
    List<Appointment> findAllWithDetails();

    @Query("SELECT a FROM Appointment a JOIN FETCH a.patient JOIN FETCH a.doctor WHERE a.doctor = :doctor ORDER BY a.appointmentDateTime")
    List<Appointment> findByDoctorWithDetails(@Param("doctor") AppUser doctor);

    boolean existsByPatientId(Long patientId);

    @Query("SELECT a FROM Appointment a WHERE a.doctor = :doctor AND a.appointmentDateTime = :datetime")
    List<Appointment> findByDoctorAndTime(@Param("doctor") AppUser doctor, @Param("datetime") LocalDateTime datetime);
}
