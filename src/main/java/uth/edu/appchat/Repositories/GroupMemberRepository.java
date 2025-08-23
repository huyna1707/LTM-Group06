package uth.edu.appchat.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uth.edu.appchat.Models.GroupChat;
import uth.edu.appchat.Models.GroupMember;

import java.util.List;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    boolean existsByGroupChatIdAndUserId(Long groupId, Long userId);
    @Query("SELECT gm.groupChat FROM GroupMember gm WHERE gm.user.id = :userId AND gm.isActive = true")
    List<GroupChat> findActiveGroupsByUserId(Long userId);

}
