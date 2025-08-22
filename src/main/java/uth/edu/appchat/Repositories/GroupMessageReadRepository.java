//package uth.edu.appchat.Repositories;
//
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import uth.edu.appchat.Models.GroupMessageRead;
//import uth.edu.appchat.Models.GroupMessage;
//import uth.edu.appchat.Models.User;
//
//import java.util.List;
//import java.util.Optional;
//
//public interface GroupMessageReadRepository extends JpaRepository<GroupMessageRead, Long> {
//
//    // Kiểm tra user đã đọc message chưa
//    Optional<GroupMessageRead> findByGroupMessageAndUser(GroupMessage groupMessage, User user);
//
//    // Lấy tất cả user đã đọc một message
//    List<GroupMessageRead> findByGroupMessage(GroupMessage groupMessage);
//
//    // Đếm số người đã đọc một message
//    @Query("SELECT COUNT(gmr) FROM GroupMessageRead gmr WHERE gmr.groupMessage = :message")
//    Long countReadersForMessage(@Param("message") GroupMessage message);
//
//    // Lấy tin nhắn chưa đọc của user trong group
//    @Query("SELECT gm FROM GroupMessage gm WHERE gm.groupChat.id = :groupId " +
//            "AND gm.sender != :user " +
//            "AND NOT EXISTS (SELECT gmr FROM GroupMessageRead gmr " +
//            "WHERE gmr.groupMessage = gm AND gmr.user = :user)")
//    List<GroupMessage> findUnreadMessagesInGroup(@Param("groupId") Long groupId, @Param("user") User user);
//}