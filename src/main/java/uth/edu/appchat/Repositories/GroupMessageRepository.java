package uth.edu.appchat.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uth.edu.appchat.Models.GroupMessage;
import uth.edu.appchat.Models.GroupChat;
import uth.edu.appchat.Models.User;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GroupMessageRepository extends JpaRepository<GroupMessage, Long> {
    
    // Tìm tin nhắn theo nhóm (sắp xếp tăng dần)
    List<GroupMessage> findByGroupChatOrderByCreatedAtAsc(GroupChat groupChat);
    
    // Tìm tin nhắn theo nhóm (sắp xếp giảm dần - tin nhắn mới nhất trước)
    List<GroupMessage> findByGroupChatOrderByCreatedAtDesc(GroupChat groupChat);

    // Tìm tin nhắn theo người gửi
    List<GroupMessage> findBySender(User sender);

    // Tìm tin nhắn theo loại
    List<GroupMessage> findByMessageType(GroupMessage.MessageType messageType);

    // Tìm tin nhắn trong khoảng thời gian
    @Query("SELECT gm FROM GroupMessage gm WHERE gm.groupChat = :groupChat AND gm.createdAt BETWEEN :startDate AND :endDate ORDER BY gm.createdAt ASC")
    List<GroupMessage> findByGroupChatAndDateRange(@Param("groupChat") GroupChat groupChat,
                                                  @Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);

    // Tìm tin nhắn gần nhất trong nhóm
    @Query("SELECT gm FROM GroupMessage gm WHERE gm.groupChat = :groupChat ORDER BY gm.createdAt DESC LIMIT :limit")
    List<GroupMessage> findRecentMessages(@Param("groupChat") GroupChat groupChat, @Param("limit") int limit);

    // Tìm tin nhắn đã ghim
    List<GroupMessage> findByGroupChatAndIsPinnedTrueOrderByPinnedAtDesc(GroupChat groupChat);

    // Đếm tin nhắn trong nhóm
    Long countByGroupChat(GroupChat groupChat);

    // Đếm tin nhắn đã ghim
    Long countByGroupChatAndIsPinnedTrue(GroupChat groupChat);
    
    // Tìm tin nhắn theo nội dung
    @Query("SELECT gm FROM GroupMessage gm WHERE gm.groupChat = :groupChat AND gm.content LIKE %:keyword%")
    List<GroupMessage> findByGroupChatAndContentContaining(@Param("groupChat") GroupChat groupChat, @Param("keyword") String keyword);
}
