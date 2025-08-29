package uth.edu.appchat.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uth.edu.appchat.Models.PrivateMessage;
import uth.edu.appchat.Models.PrivateChat;
import uth.edu.appchat.Models.User;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PrivateMessageRepository extends JpaRepository<PrivateMessage, Long> {

    // Tìm tin nhắn theo chat riêng
    List<PrivateMessage> findByPrivateChatOrderByCreatedAtAsc(PrivateChat privateChat);

    // Tìm tin nhắn theo người gửi
    List<PrivateMessage> findBySender(User sender);

    // Tìm tin nhắn theo loại
    List<PrivateMessage> findByMessageType(PrivateMessage.MessageType messageType);

    // Tìm tin nhắn trong khoảng thời gian
    @Query("SELECT pm FROM PrivateMessage pm WHERE pm.privateChat = :chat AND pm.createdAt BETWEEN :startDate AND :endDate ORDER BY pm.createdAt ASC")
    List<PrivateMessage> findByPrivateChatAndDateRange(@Param("chat") PrivateChat chat,
                                                       @Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate);

    // Tìm tin nhắn gần nhất
    @Query("SELECT pm FROM PrivateMessage pm WHERE pm.privateChat = :chat ORDER BY pm.createdAt DESC LIMIT :limit")
    List<PrivateMessage> findRecentMessages(@Param("chat") PrivateChat chat, @Param("limit") int limit);

    // Đếm tin nhắn chưa đọc
    @Query("SELECT COUNT(pm) FROM PrivateMessage pm WHERE pm.privateChat = :chat AND pm.sender != :user AND pm.isRead = false")
    Long countUnreadMessages(@Param("chat") PrivateChat chat, @Param("user") User user);

    // Đánh dấu đã đọc
    @Query("UPDATE PrivateMessage pm SET pm.isRead = true WHERE pm.privateChat = :chat AND pm.sender != :user")
    void markAsRead(@Param("chat") PrivateChat chat, @Param("user") User user);

    // Tìm tin nhắn chưa đọc của user
    @Query("SELECT pm FROM PrivateMessage pm WHERE pm.privateChat = :chat AND pm.sender != :user AND pm.isRead = false ORDER BY pm.createdAt ASC")
    List<PrivateMessage> findByPrivateChatAndSenderNotAndIsReadFalse(@Param("chat") PrivateChat chat, @Param("user") User user);

    // Đánh dấu đã đọc với transaction
    @Modifying
    @Transactional
    @Query("UPDATE PrivateMessage pm SET pm.isRead = true, pm.readAt = :readTime WHERE pm.privateChat = :chat AND pm.sender != :user AND pm.isRead = false")
    void markAsReadWithTime(@Param("chat") PrivateChat chat, @Param("user") User user, @Param("readTime") LocalDateTime readTime);

    // Lấy tin nhắn theo thứ tự giảm dần để rollback
    @Query("SELECT pm FROM PrivateMessage pm WHERE pm.privateChat = :chat ORDER BY pm.createdAt DESC")
    List<PrivateMessage> findByPrivateChatOrderByCreatedAtDesc(@Param("chat") PrivateChat chat);
}