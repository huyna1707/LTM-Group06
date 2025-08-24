package uth.edu.appchat.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uth.edu.appchat.Models.GroupChat;
import uth.edu.appchat.Models.GroupMember;

import java.util.List;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    @Query("SELECT gm.groupChat FROM GroupMember gm WHERE gm.user.id = :userId AND gm.isActive = true")
    List<GroupChat> findActiveGroupsByUserId(Long userId);

    boolean existsByGroupChatIdAndUserId(Long groupChatId, Long userId);

    boolean existsByGroupChatIdAndUserIdAndIsActive(Long groupChatId, Long userId, boolean isActive);
}