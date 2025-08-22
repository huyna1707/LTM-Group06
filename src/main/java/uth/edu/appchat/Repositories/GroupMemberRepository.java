package uth.edu.appchat.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import uth.edu.appchat.Models.GroupMember;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    boolean existsByGroupChatIdAndUserId(Long groupId, Long userId);
}
