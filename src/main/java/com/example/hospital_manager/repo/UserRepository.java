package com.example.hospital_manager.repo;

import com.example.hospital_manager.model.AppUser;
import com.example.hospital_manager.model.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query("SELECT u FROM AppUser u WHERE u.role = :role ORDER BY u.username")
    List<AppUser> findByRole(Role role);
}
