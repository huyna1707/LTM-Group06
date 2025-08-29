package uth.edu.appchat.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;   // <-- nhớ import Param
import org.springframework.stereotype.Repository;
import uth.edu.appchat.Models.GroupChat;
import uth.edu.appchat.Models.GroupMember;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {


    @Query("""
           SELECT gm.groupChat
           FROM GroupMember gm
           WHERE gm.user.id = :userId AND gm.isActive = true
           """)
    List<GroupChat> findActiveGroupsByUserId(@Param("userId") Long userId);

    boolean existsByGroupChatIdAndUserId(Long groupChatId, Long userId);
    boolean existsByGroupChatIdAndUserIdAndIsActive(Long groupChatId, Long userId, boolean isActive);

    // === THÊM CHO TÍNH NĂNG BIỆT DANH ===

    // Lấy danh sách thành viên (kèm user) để hiển thị & sửa nickname, tránh N+1
    @Query("""
           SELECT gm
           FROM GroupMember gm
           JOIN FETCH gm.user u
           WHERE gm.groupChat.id = :groupId
           ORDER BY gm.joinedAt ASC
           """)
    List<GroupMember> findByGroupIdWithUser(@Param("groupId") Long groupId);


    // Tìm 1 thành viên trong nhóm để cập nhật nickname
    Optional<GroupMember> findByGroupChatIdAndUserId(Long groupId, Long userId);
    // Trả về danh sách username của các thành viên đang active trong group
    @Query("""
       select gm.user.username
       from GroupMember gm
       where gm.groupChat.id = :groupId
         and gm.isActive = true
       """)
    List<String> findActiveUsernames(@Param("groupId") Long groupId);
}
