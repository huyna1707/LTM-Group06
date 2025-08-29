package uth.edu.appchat.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uth.edu.appchat.Models.GroupMessage;

import java.util.List;

@Repository
public interface GroupMessageRepository extends JpaRepository<GroupMessage, Long> {
    List<GroupMessage> findByGroupChatIdOrderByCreatedAtAsc(Long groupChatId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT gm.sender.id) FROM GroupMessage gm WHERE gm.groupChat.id = :groupId AND gm.createdAt BETWEEN :startDate AND :endDate")
    Long countDistinctSendersInRange(@org.springframework.data.repository.query.Param("groupId") Long groupId, @org.springframework.data.repository.query.Param("startDate") java.time.LocalDateTime startDate, @org.springframework.data.repository.query.Param("endDate") java.time.LocalDateTime endDate);
}