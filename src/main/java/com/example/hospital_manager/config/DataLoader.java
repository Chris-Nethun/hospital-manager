package com.example.hospital_manager.config;

import com.example.hospital_manager.model.AppUser;
import com.example.hospital_manager.model.Patient;
import com.example.hospital_manager.model.Role;
import com.example.hospital_manager.repo.PatientRepository;
import com.example.hospital_manager.repo.UserRepository;
import java.time.LocalDate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataLoader {

    @Bean
    CommandLineRunner seedData(UserRepository userRepository,
                               PatientRepository patientRepository,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() == 0) {
                userRepository.save(AppUser.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin123"))
                        .role(Role.ADMIN)
                        .build());

                userRepository.save(AppUser.builder()
                        .username("doctor1")
                        .password(passwordEncoder.encode("doctor123"))
                        .role(Role.DOCTOR)
                        .build());

                userRepository.save(AppUser.builder()
                        .username("nurse1")
                        .password(passwordEncoder.encode("nurse123"))
                        .role(Role.NURSE)
                        .build());

                userRepository.save(AppUser.builder()
                        .username("receptionist1")
                        .password(passwordEncoder.encode("receptionist123"))
                        .role(Role.RECEPTIONIST)
                        .build());
            }

            if (patientRepository.count() == 0) {
                patientRepository.save(Patient.builder()
                        .firstName("Alice")
                        .lastName("Johnson")
                        .email("alice.johnson@example.com")
                        .phone("+1234567890")
                        .dateOfBirth(LocalDate.of(1988, 5, 12))
                        .gender("Female")
                        .address("123 Main St")
                        .emergencyContact("John Johnson")
                        .build());

                patientRepository.save(Patient.builder()
                        .firstName("Bob")
                        .lastName("Smith")
                        .email("bob.smith@example.com")
                        .phone("+1987654321")
                        .dateOfBirth(LocalDate.of(1992, 11, 3))
                        .gender("Male")
                        .address("456 Oak Ave")
                        .emergencyContact("Mary Smith")
                        .build());
            }
        };
    }
}
