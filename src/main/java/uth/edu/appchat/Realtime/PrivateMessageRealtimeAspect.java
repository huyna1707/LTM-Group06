// uth/edu/appchat/Realtime/PrivateMessageRealtimeAspect.java
package uth.edu.appchat.Realtime;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import uth.edu.appchat.Models.PrivateChat;
import uth.edu.appchat.Models.PrivateMessage;
import uth.edu.appchat.Models.User;

import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor
public class PrivateMessageRealtimeAspect {

    private final SimpMessagingTemplate ws;

    // Chạy sau khi save(PrivateMessage) thành công
    @AfterReturning(
            pointcut = "execution(* uth.edu.appchat.Repositories.PrivateMessageRepository.save(..)) && args(entity)",
            returning = "saved"
    )
    public void afterSavePrivateMessage(PrivateMessage saved, Object entity) {
        if (saved == null) return;

        Runnable push = () -> {
            PrivateChat chat = saved.getPrivateChat();
            if (chat == null) return;
            User sender = saved.getSender();
            if (sender == null) return;
            User other = chat.getOtherUser(sender);
            if (other == null) return;

            Map<String, Object> payload = Map.of(
                    "id", saved.getId(),
                    "content", saved.getContent(),
                    "sender", Map.of(
                            "id", sender.getId(),
                            "username", sender.getUsername(),
                            "fullName", sender.getFullName() != null ? sender.getFullName() : sender.getUsername()
                    ),
                    "timestamp", saved.getCreatedAt() != null ? saved.getCreatedAt().toString() : null,
                    "messageType", saved.getMessageType().toString(),
                    "chatId", chat.getId()
            );

            // Client sub: /user/queue/private
            ws.convertAndSendToUser(other.getUsername(), "/queue/private", payload);
            // tuỳ chọn: đồng bộ cho chính sender (multi-device)
            ws.convertAndSendToUser(sender.getUsername(), "/queue/private", payload);
        };

        // Gửi sau khi COMMIT để tránh “ảo ảnh” khi rollback
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { push.run(); }
            });
        } else {
            push.run();
        }
    }
}
