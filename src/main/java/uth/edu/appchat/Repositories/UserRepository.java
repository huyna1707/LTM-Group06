package uth.edu.appchat.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uth.edu.appchat.Models.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // Original methods that might cause duplicate issues
    @Query("SELECT u FROM User u WHERE u.username = :username ORDER BY u.id ASC")
    List<User> findAllByUsername(@Param("username") String username);

    @Query("SELECT u FROM User u WHERE u.email = :email ORDER BY u.id ASC")
    List<User> findAllByEmail(@Param("email") String email);

    // Safe wrapper methods that handle duplicates
    default Optional<User> findByUsername(String username) {
        List<User> users = findAllByUsername(username);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    default Optional<User> findByEmail(String email) {
        List<User> users = findAllByEmail(email);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
