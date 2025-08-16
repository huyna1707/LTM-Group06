package uth.edu.appchat.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import uth.edu.appchat.Models.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}