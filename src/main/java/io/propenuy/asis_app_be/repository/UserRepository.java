package io.propenuy.asis_app_be.repository;

import io.propenuy.asis_app_be.model.User;
import java.util.UUID;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByUsername(String username);
    List<User> findByNamaContainingIgnoreCase(String nama);
    List<User> findByRole(String role);
    List<User> findAll();
    Optional<User> findByUsername(String username);
}