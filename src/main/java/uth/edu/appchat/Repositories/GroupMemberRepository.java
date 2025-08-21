package uth.edu.appchat.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uth.edu.appchat.Models.GroupMember;
import uth.edu.appchat.Models.GroupChat;
import uth.edu.appchat.Models.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    
    // Tìm thành viên active theo nhóm
    List<GroupMember> findByGroupChatAndIsActiveTrue(GroupChat groupChat);
    
    // Tìm các nhóm mà user tham gia (active)
    List<GroupMember> findByUserAndIsActiveTrue(User user);
    
    // Kiểm tra user có trong nhóm không
    Optional<GroupMember> findByGroupChatAndUser(GroupChat groupChat, User user);
    
    // Kiểm tra user có trong nhóm và active không
    Optional<GroupMember> findByGroupChatAndUserAndIsActiveTrue(GroupChat groupChat, User user);
    
    // Đếm số thành viên active
    Long countByGroupChatAndIsActiveTrue(GroupChat groupChat);
    
    // Tìm admin của nhóm
    @Query("SELECT gm FROM GroupMember gm WHERE gm.groupChat = :groupChat AND gm.role = 'ADMIN' AND gm.isActive = true")
    List<GroupMember> findAdminsByGroupChat(@Param("groupChat") GroupChat groupChat);
    
    // Tìm moderator của nhóm
    @Query("SELECT gm FROM GroupMember gm WHERE gm.groupChat = :groupChat AND gm.role = 'MODERATOR' AND gm.isActive = true")
    List<GroupMember> findModeratorsByGroupChat(@Param("groupChat") GroupChat groupChat);
    
    // Kiểm tra user có phải admin không
    @Query("SELECT CASE WHEN COUNT(gm) > 0 THEN true ELSE false END " +
           "FROM GroupMember gm WHERE gm.groupChat = :groupChat AND gm.user = :user AND gm.role = 'ADMIN' AND gm.isActive = true")
    Boolean isAdmin(@Param("groupChat") GroupChat groupChat, @Param("user") User user);
    
    // Kiểm tra user có quyền moderator trở lên không
    @Query("SELECT CASE WHEN COUNT(gm) > 0 THEN true ELSE false END " +
           "FROM GroupMember gm WHERE gm.groupChat = :groupChat AND gm.user = :user AND gm.role IN ('ADMIN', 'MODERATOR') AND gm.isActive = true")
    Boolean hasModeratorRights(@Param("groupChat") GroupChat groupChat, @Param("user") User user);
}
