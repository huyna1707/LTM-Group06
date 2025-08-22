package uth.edu.appchat.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uth.edu.appchat.Models.ChatStreak;
import uth.edu.appchat.Models.GroupChat;
import uth.edu.appchat.Models.PrivateChat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatStreakRepository extends JpaRepository<ChatStreak, Long> {

    /**
     * Tìm streak cho private chat
     */
    Optional<ChatStreak> findByPrivateChat(PrivateChat privateChat);

    /**
     * Tìm streak cho group chat
     */
    Optional<ChatStreak> findByGroupChat(GroupChat groupChat);

    /**
     * Tìm tất cả streak cần kiểm tra reset (quá lâu không nhắn)
     */
    @Query("SELECT cs FROM ChatStreak cs WHERE " +
           "cs.lastMessageDate IS NOT NULL AND " +
           "cs.currentStreak > 0 AND " +
           "cs.lastMessageDate < :cutoffDate")
    List<ChatStreak> findStreaksToCheck(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * Tìm streak theo private chat ID
     */
    @Query("SELECT cs FROM ChatStreak cs WHERE cs.privateChat.id = :privateChatId")
    Optional<ChatStreak> findByPrivateChatId(@Param("privateChatId") Long privateChatId);

    /**
     * Tìm streak theo group chat ID
     */
    @Query("SELECT cs FROM ChatStreak cs WHERE cs.groupChat.id = :groupChatId")
    Optional<ChatStreak> findByGroupChatId(@Param("groupChatId") Long groupChatId);

    /**
     * Lấy top streak của private chats
     */
    @Query("SELECT cs FROM ChatStreak cs WHERE " +
           "cs.privateChat IS NOT NULL AND " +
           "cs.currentStreak >= 3 " +
           "ORDER BY cs.currentStreak DESC")
    List<ChatStreak> findTopPrivateStreaks();

    /**
     * Lấy top streak của group chats
     */
    @Query("SELECT cs FROM ChatStreak cs WHERE " +
           "cs.groupChat IS NOT NULL AND " +
           "cs.currentStreak >= 3 " +
           "ORDER BY cs.currentStreak DESC")
    List<ChatStreak> findTopGroupStreaks();
}
