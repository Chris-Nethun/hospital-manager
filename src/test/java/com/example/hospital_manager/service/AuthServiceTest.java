package com.example.hospital_manager.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.hospital_manager.dto.AuthResponse;
import com.example.hospital_manager.dto.LoginRequest;
import com.example.hospital_manager.dto.RegisterRequest;
import com.example.hospital_manager.dto.UserResponse;
import com.example.hospital_manager.exception.ApiException;
import com.example.hospital_manager.model.AppUser;
import com.example.hospital_manager.model.Role;
import com.example.hospital_manager.repo.UserRepository;
import com.example.hospital_manager.security.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @InjectMocks private AuthService authService;

    @Test
    void registerPatient_savesUserWithFullNameAndHashedPassword_noPatientIdRequired() {
        when(userRepository.existsByUsername("patient_test")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedSecret");
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser user = invocation.getArgument(0);
            user.setId(8L);
            return user;
        });

        UserResponse response = authService.register(
                new RegisterRequest("patient_test", "password123", "Test User", Role.PATIENT));

        ArgumentCaptor<AppUser> savedUser = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(savedUser.capture());
        assertEquals("patient_test", savedUser.getValue().getUsername());
        assertEquals("Test User", savedUser.getValue().getFullName());
        assertEquals("encodedSecret", savedUser.getValue().getPassword());
        assertEquals(Role.PATIENT, response.role());
        assertEquals(8L, response.id());
        assertEquals("Test User", response.fullName());
    }

    @Test
    void register_throwsConflict_whenUsernameAlreadyExists() {
        when(userRepository.existsByUsername("patient_test")).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class,
                () -> authService.register(new RegisterRequest("patient_test", "pass", "Test", Role.PATIENT)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void register_rejectsNurseRole_whenNotAdmin() {
        ApiException exception = assertThrows(ApiException.class,
                () -> authService.register(new RegisterRequest("nurse1", "pass", "Nurse Test", Role.NURSE)));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    void login_returnsTokenAndUserInformation() {
        AppUser user = AppUser.builder()
                .id(1L)
                .username("patient_test")
                .password("encoded")
                .fullName("Test User")
                .role(Role.PATIENT)
                .build();

        when(userRepository.findByUsername("patient_test")).thenReturn(Optional.of(user));
        when(jwtService.generateToken("patient_test", Role.PATIENT)).thenReturn("mocked.jwt.token");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);

        AuthResponse response = authService.login(new LoginRequest("patient_test", "password123"));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        assertEquals("mocked.jwt.token", response.token());
        assertEquals("Bearer", response.tokenType());
        assertEquals(3600000L, response.expiresInMs());
        assertEquals("patient_test", response.username());
        assertEquals("Test User", response.fullName());
        assertEquals(Role.PATIENT, response.role());
    }
}
