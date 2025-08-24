package uth.edu.appchat.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uth.edu.appchat.Models.PrivateChat;
import uth.edu.appchat.Models.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrivateChatRepository extends JpaRepository<PrivateChat, Long> {

    // Tìm chat riêng giữa 2 người - sử dụng DISTINCT và LIMIT để tránh lỗi duplicate
    @Query("SELECT DISTINCT pc FROM PrivateChat pc WHERE " +
            "(pc.user1.id = :user1Id AND pc.user2.id = :user2Id) OR " +
            "(pc.user1.id = :user2Id AND pc.user2.id = :user1Id) " +
            "ORDER BY pc.id ASC")
    List<PrivateChat> findByUserIds(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

    // Wrapper method để trả về Optional và chỉ lấy kết quả đầu tiên
    default Optional<PrivateChat> findByUsers(User user1, User user2) {
        List<PrivateChat> results = findByUserIds(user1.getId(), user2.getId());
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    // Tìm tất cả chat riêng của 1 user
    @Query("SELECT pc FROM PrivateChat pc WHERE pc.user1 = :user OR pc.user2 = :user " +
            "ORDER BY pc.lastMessageAt DESC")
    List<PrivateChat> findByUser(@Param("user") User user);
}