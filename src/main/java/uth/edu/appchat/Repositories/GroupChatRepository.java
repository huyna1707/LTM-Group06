package uth.edu.appchat.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uth.edu.appchat.Models.GroupChat;
import uth.edu.appchat.Models.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupChatRepository extends JpaRepository<GroupChat, Long> {

    // Tìm nhóm theo người tạo
    List<GroupChat> findByCreatedBy(User createdBy);

    // Tìm nhóm theo tên chính xác
    Optional<GroupChat> findByName(String name);

    // Tìm nhóm theo tên (tìm kiếm gần đúng)
    List<GroupChat> findByNameContainingIgnoreCase(String name);

    // Tìm các nhóm mà user tham gia
    @Query("SELECT gm.groupChat FROM GroupMember gm WHERE gm.user = :user AND gm.isActive = true " +
           "ORDER BY gm.groupChat.lastMessageAt DESC")
    List<GroupChat> findByUser(@Param("user") User user);

    // Tìm nhóm mà user là admin
    @Query("SELECT gm.groupChat FROM GroupMember gm WHERE gm.user = :user AND gm.role = 'ADMIN' AND gm.isActive = true")
    List<GroupChat> findByUserAsAdmin(@Param("user") User user);

    // Đếm số thành viên active trong nhóm
    @Query("SELECT COUNT(gm) FROM GroupMember gm WHERE gm.groupChat = :groupChat AND gm.isActive = true")
    Long countActiveMembers(@Param("groupChat") GroupChat groupChat);

    // Tìm nhóm có ít nhất n thành viên
    @Query("SELECT gc FROM GroupChat gc WHERE " +
           "(SELECT COUNT(gm) FROM GroupMember gm WHERE gm.groupChat = gc AND gm.isActive = true) >= :minMembers")
    List<GroupChat> findGroupsWithMinMembers(@Param("minMembers") int minMembers);
}
