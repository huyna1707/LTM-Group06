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
import uth.edu.appchat.Models.User;
import uth.edu.appchat.Repositories.GroupChatRepository;
import uth.edu.appchat.Repositories.GroupMemberRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Aspect
@Component
@RequiredArgsConstructor
public class GroupMessageRealtimeAspect {

    private final SimpMessagingTemplate ws;
    private final GroupMemberRepository groupMemberRepo;

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