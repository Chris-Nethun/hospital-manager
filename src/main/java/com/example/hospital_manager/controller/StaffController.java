package com.example.hospital_manager.controller;

import com.example.hospital_manager.model.AppUser;
import com.example.hospital_manager.model.Role;
import com.example.hospital_manager.repo.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class StaffController {

    private final UserRepository userRepository;

    public StaffController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/users")
    public List<AppUser> getUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/doctors")
    public List<AppUser> getDoctors() {
        return userRepository.findByRole(Role.DOCTOR);
    }
}
