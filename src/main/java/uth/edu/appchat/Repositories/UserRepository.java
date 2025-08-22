package uth.edu.appchat.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uth.edu.appchat.Models.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // ✅ Chỉ cần 1 giá trị → kiểm tra username, phone hoặc email
    @Query("SELECT u FROM User u WHERE u.username = :value OR u.phone = :value OR u.email = :value")
    Optional<User> findByUsernameOrPhoneOrEmail(@Param("value") String value);

    // Các hàm hỗ trợ tìm tất cả theo username/email
    List<User> findAllByUsername(String username);
    List<User> findAllByEmail(String email);

    // Safe wrapper nếu có duplicate
    default Optional<User> findByUsername(String username) {
        List<User> users = findAllByUsername(username);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    default Optional<User> findByEmail(String email) {
        List<User> users = findAllByEmail(email);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    // Kiểm tra tồn tại
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
