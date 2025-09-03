// src/main/java/uth/edu/appchat/Repositories/PrivateMessageRepository.java
package uth.edu.appchat.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import uth.edu.appchat.Models.PrivateMessage;
import uth.edu.appchat.Models.PrivateChat;

import java.time.LocalDateTime;
import java.util.List;

public interface PrivateMessageRepository extends JpaRepository<PrivateMessage, Long> {

    // Lấy tin nhắn của 1 chat, mới hơn mốc (clearedAt), tăng dần theo thời gian
    List<PrivateMessage> findByPrivateChatAndCreatedAtAfterOrderByCreatedAtAsc(
            PrivateChat chat,
            LocalDateTime clearedAt
    );
}
