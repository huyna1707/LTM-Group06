package uth.edu.appchat.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uth.edu.appchat.Models.GroupMessage;

import java.util.List;

@Repository
public interface GroupMessageRepository extends JpaRepository<GroupMessage, Long> {
    List<GroupMessage> findByGroupChatIdOrderByCreatedAtAsc(Long groupChatId);
}