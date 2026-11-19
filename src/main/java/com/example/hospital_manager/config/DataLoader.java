package com.example.hospital_manager.config;

import com.example.hospital_manager.model.AppUser;
import com.example.hospital_manager.model.Patient;
import com.example.hospital_manager.model.Role;
import com.example.hospital_manager.repo.PatientRepository;
import com.example.hospital_manager.repo.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seeds the database with default accounts and sample patients on first startup.
 *
 * <pre>
 * Default accounts:
 *   admin          / admin123        (ADMIN)
 *   doctor1        / doctor123       (DOCTOR)
 *   nurse1         / nurse123        (NURSE)
 *   receptionist1  / receptionist123 (RECEPTIONIST)
 *   patient1       / patient123      (PATIENT — linked to Alice Johnson)
 * </pre>
 */
@Configuration
public class DataLoader {

    @Bean
    @Order(1)
    CommandLineRunner fixRoleSchema(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                // Drop any existing CHECK constraints on app_users (such as old enum check constraint)
                List<String> checkConstraints = jdbcTemplate.query(
                        "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
                        "WHERE UPPER(TABLE_NAME) = 'APP_USERS' AND CONSTRAINT_TYPE = 'CHECK'",
                        (rs, rowNum) -> rs.getString("CONSTRAINT_NAME")
                );
                for (String constraintName : checkConstraints) {
                    try {
                        jdbcTemplate.execute("ALTER TABLE app_users DROP CONSTRAINT \"" + constraintName + "\"");
                    } catch (Exception ignored) {
                    }
                }
                // Alter role column to plain VARCHAR(50) so all roles (including PATIENT) are accepted
                jdbcTemplate.execute("ALTER TABLE app_users ALTER COLUMN role VARCHAR(50)");
            } catch (Exception ignored) {
            }
        };
    }

    @Bean
    @Order(2)
    CommandLineRunner seedData(UserRepository userRepository,
                               PatientRepository patientRepository,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() == 0) {
                // ── Staff accounts ──────────────────────────────────────────────
                userRepository.save(AppUser.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin123"))
                        .fullName("System Administrator")
                        .role(Role.ADMIN)
                        .build());

                userRepository.save(AppUser.builder()
                        .username("doctor1")
                        .password(passwordEncoder.encode("doctor123"))
                        .fullName("Dr. Doctor One")
                        .role(Role.DOCTOR)
                        .build());

                userRepository.save(AppUser.builder()
                        .username("nurse1")
                        .password(passwordEncoder.encode("nurse123"))
                        .fullName("Nurse One")
                        .role(Role.NURSE)
                        .build());

                userRepository.save(AppUser.builder()
                        .username("receptionist1")
                        .password(passwordEncoder.encode("receptionist123"))
                        .fullName("Receptionist One")
                        .role(Role.RECEPTIONIST)
                        .build());
            }

            if (patientRepository.count() == 0) {
                // ── Sample patients ─────────────────────────────────────────────
                Patient alice = patientRepository.save(Patient.builder()
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

                // ── Patient portal account (linked to Alice) ────────────────────
                if (!userRepository.existsByUsername("patient1")) {
                    userRepository.save(AppUser.builder()
                            .username("patient1")
                            .password(passwordEncoder.encode("patient123"))
                            .fullName("Alice Johnson")
                            .role(Role.PATIENT)
                            .patientProfile(alice)
                            .build());
                }
            }
        };
    }
}
