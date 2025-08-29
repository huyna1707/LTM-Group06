// uth/edu/appchat/Realtime/GroupMessageRealtimeAspect.java
package uth.edu.appchat.Realtime;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import uth.edu.appchat.Models.GroupChat;
import uth.edu.appchat.Models.GroupMessage;
import uth.edu.appchat.Repositories.GroupChatRepository;
import uth.edu.appchat.Repositories.GroupMemberRepository;
import uth.edu.appchat.Repositories.GroupMessageRepository;

import java.util.List;
import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor
public class GroupMessageRealtimeAspect {

    private final SimpMessagingTemplate ws;
    private final GroupMemberRepository groupMemberRepo;
    private final GroupChatRepository groupChatRepo;
    private final GroupMessageRepository groupMessageRepo;

    @AfterReturning(
            pointcut = "execution(* uth.edu.appchat.Repositories.GroupMessageRepository.save(..)) && args(entity)",
            returning = "saved"
    )
    public void afterSaveGroupMessage(GroupMessage saved, Object entity) {
        if (saved == null || saved.getGroupChat() == null) return;
        Long groupId = saved.getGroupChat().getId();

        Runnable push = () -> {
            // LẤY USERNAME ACTIVE
            List<String> usernames = groupMemberRepo.findActiveUsernames(groupId);

            Map<String, Object> dto = Map.of(
                    "id",        saved.getId(),
                    "content",   saved.getContent(),
                    "sender",    Map.of(
                            "username", saved.getSender()!=null ? saved.getSender().getUsername() : null,
                            "fullName", (saved.getSender()!=null && saved.getSender().getFullName()!=null)
                                    ? saved.getSender().getFullName()
                                    : (saved.getSender()!=null ? saved.getSender().getUsername() : "")
                    ),
                    "timestamp", saved.getCreatedAt()!=null ? saved.getCreatedAt().toString() : null,
                    "groupId",   groupId
            );

            // Update streak for group only if at least 2 distinct senders today
            try {
                GroupChat group = saved.getGroupChat();
                java.time.LocalDate today = java.time.LocalDate.now();
                java.time.LocalDateTime start = today.atStartOfDay();
                java.time.LocalDateTime end = today.atTime(23,59,59,999_999_999);

                Long distinctSenders = groupMessageRepo.countDistinctSendersInRange(group.getId(), start, end);
                // Require at least 2 distinct senders for group streak to be active
                boolean sufficient = distinctSenders != null && distinctSenders >= 2;

                if (sufficient) {
                    java.time.LocalDate last = group.getStreakLastDate();
                    Integer cnt = group.getStreakCount() == null ? 0 : group.getStreakCount();
                    if (last == null) cnt = 1;
                    else if (last.equals(today)) { /* already counted */ }
                    else if (last.plusDays(1).equals(today)) cnt = cnt + 1;
                    else cnt = 1;
                    group.setStreakCount(cnt);
                    group.setStreakLastDate(today);
                    if (group.getStreakRecoveryMonth() == null || group.getStreakRecoveryYear() == null ||
                            group.getStreakRecoveryMonth() != today.getMonthValue() || group.getStreakRecoveryYear() != today.getYear()) {
                        group.setStreakRecoveryMonth(today.getMonthValue());
                        group.setStreakRecoveryYear(today.getYear());
                        group.setStreakRecoveryUsed(0);
                    }
                    groupChatRepo.save(group);
                }

                // attach streakCount and flag
                Map<String, Object> withStreak = new java.util.HashMap<>(dto);
                withStreak.put("streakCount", group.getStreakCount());
                withStreak.put("sufficientSendersToday", sufficient);
                withStreak.put("streakLastDate", group.getStreakLastDate()!=null?group.getStreakLastDate().toString():null);
                dto = Map.copyOf(withStreak);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Client subscribe: /user/queue/group
            for (String u : usernames) {
                ws.convertAndSendToUser(u, "/queue/group", dto);
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { push.run(); }
            });
        } else {
            push.run();
        }
    }
}