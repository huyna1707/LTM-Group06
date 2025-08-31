package uth.edu.appchat.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("""
       SELECT gm
       FROM GroupMember gm
       JOIN FETCH gm.user u
       WHERE gm.groupChat.id = :groupId
         AND gm.isActive = true
       ORDER BY gm.joinedAt ASC
       """)
    List<GroupMember> findByGroupIdWithUser(@Param("groupId") Long groupId);

    // ✅ CHỈ GIỮ 1 BẢN NÀY
    Optional<GroupMember> findByGroupChatIdAndUserId(Long groupId, Long userId);

    // ===== dùng cho leave/delete/avatar, v.v. =====
    Optional<GroupMember> findByGroupChatIdAndUserUsernameAndIsActiveTrue(Long groupChatId, String username);
    long countByGroupChatIdAndIsActiveTrue(Long groupChatId);
    long countByGroupChatIdAndRoleAndIsActiveTrue(Long groupChatId, GroupMember.GroupRole role);
    Optional<GroupMember> findFirstByGroupChatIdAndIsActiveTrueOrderByJoinedAtAsc(Long groupId);

    @Query("""
       select gm.user.username
       from GroupMember gm
       where gm.groupChat.id = :groupId
         and gm.isActive = true
       """)
    List<String> findActiveUsernames(@Param("groupId") Long groupId);
}
