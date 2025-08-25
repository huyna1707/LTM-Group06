package uth.edu.appchat.Services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import uth.edu.appchat.Models.GroupChat;
import uth.edu.appchat.Models.PrivateChat;
import uth.edu.appchat.Repositories.GroupChatRepository;
import uth.edu.appchat.Repositories.PrivateChatRepository;
import uth.edu.appchat.Dtos.NicknameResponse;
import jakarta.persistence.EntityNotFoundException;

import java.time.LocalDateTime;

@Service
public class ChatNicknameService {
    private final GroupChatRepository groupRepo;
    private final PrivateChatRepository privateRepo;
    private final SimpMessagingTemplate ws;

    public ChatNicknameService(GroupChatRepository g, PrivateChatRepository p, SimpMessagingTemplate ws) {
        this.groupRepo = g; this.privateRepo = p; this.ws = ws;
    }

    @Transactional
    public NicknameResponse updateGroup(Long groupId, String raw, Long actorUserId) {
        String nickname = normalize(raw);
        GroupChat gc = groupRepo.findById(groupId).orElseThrow(() -> new EntityNotFoundException("Group không tồn tại"));
        // TODO: kiểm tra quyền: actorUserId là member/moderator của group
        gc.setNickname(nickname);
        gc.setNicknameUpdatedBy(actorUserId);
        gc.setNicknameUpdatedAt(LocalDateTime.now());
        GroupChat saved = groupRepo.save(gc);

        NicknameResponse resp = new NicknameResponse(saved.getId(),"GROUP",saved.getNickname(),
                saved.getNicknameUpdatedBy(), saved.getNicknameUpdatedAt(), saved.getVersion());

        ws.convertAndSend("/topic/groups/" + groupId + "/nickname", resp);
        return resp;
    }

    @Transactional
    public NicknameResponse updatePrivate(Long chatId, String raw, Long actorUserId) {
        String nickname = normalize(raw);
        PrivateChat pc = privateRepo.findById(chatId).orElseThrow(() -> new EntityNotFoundException("Private chat không tồn tại"));
        // TODO: kiểm tra quyền: actorUserId là 1 trong 2 thành viên
        pc.setNickname(nickname);
        pc.setNicknameUpdatedBy(actorUserId);
        pc.setNicknameUpdatedAt(LocalDateTime.now());
        PrivateChat saved = privateRepo.save(pc);

        NicknameResponse resp = new NicknameResponse(saved.getId(),"PRIVATE",saved.getNickname(),
                saved.getNicknameUpdatedBy(), saved.getNicknameUpdatedAt(), saved.getVersion());

        ws.convertAndSend("/topic/private/" + chatId + "/nickname", resp);
        return resp;
    }

    private String normalize(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        return s.isBlank() ? null : s; // cho phép xóa biệt danh bằng chuỗi rỗng
    }
}
