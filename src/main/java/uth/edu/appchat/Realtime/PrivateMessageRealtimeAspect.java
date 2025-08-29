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
import uth.edu.appchat.Repositories.PrivateChatRepository;
import uth.edu.appchat.Repositories.PrivateMessageRepository;

import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor
public class PrivateMessageRealtimeAspect {

    private final SimpMessagingTemplate ws;
    private final PrivateChatRepository privateChatRepo;
    private final PrivateMessageRepository privateMessageRepo;

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

            // Update streak state on chat only if both participants have sent a message today
            try {
                java.time.LocalDate today = java.time.LocalDate.now();
                java.time.LocalDateTime start = today.atStartOfDay();
                java.time.LocalDateTime end = today.atTime(23,59,59,999_999_999);

                Long distinctSenders = privateMessageRepo.countDistinctSendersInRange(chat, start, end);
                // For private chat, require both participants to have sent a message today -> distinct senders >= 2
                boolean sufficient = distinctSenders != null && distinctSenders >= 2;

                if (sufficient) {
                    java.time.LocalDate last = chat.getStreakLastDate();
                    Integer cnt = chat.getStreakCount() == null ? 0 : chat.getStreakCount();

                    if (last == null) {
                        cnt = 1;
                    } else if (last.equals(today)) {
                        // already counted for today
                    } else if (last.plusDays(1).equals(today)) {
                        cnt = cnt + 1;
                    } else {
                        // gap > 1 day -> streak broken
                        cnt = 1;
                    }

                    chat.setStreakCount(cnt);
                    chat.setStreakLastDate(today);
                    // reset monthly recovery usage if month changed
                    int m = today.getMonthValue(); int y = today.getYear();
                    if (chat.getStreakRecoveryMonth() == null || chat.getStreakRecoveryYear() == null ||
                            chat.getStreakRecoveryMonth() != m || chat.getStreakRecoveryYear() != y) {
                        chat.setStreakRecoveryMonth(m);
                        chat.setStreakRecoveryYear(y);
                        chat.setStreakRecoveryUsed(0);
                    }
                    privateChatRepo.save(chat);
                }

                // always include streak info in payload so UI can render gray/orange accordingly
                payload = Map.of(
                        "id", saved.getId(),
                        "content", saved.getContent(),
                        "sender", Map.of(
                                "id", sender.getId(),
                                "username", sender.getUsername(),
                                "fullName", sender.getFullName() != null ? sender.getFullName() : sender.getUsername()
                        ),
                        "timestamp", saved.getCreatedAt() != null ? saved.getCreatedAt().toString() : null,
                        "messageType", saved.getMessageType().toString(),
                        "chatId", chat.getId(),
                        "streakCount", chat.getStreakCount(),
                        "streakLastDate", chat.getStreakLastDate() != null ? chat.getStreakLastDate().toString() : null,
                        "sufficientSendersToday", sufficient
                );
            } catch (Exception ex) {
                ex.printStackTrace();
            }

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
