package uth.edu.appchat.Services;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uth.edu.appchat.Dtos.NicknameResponse;
import uth.edu.appchat.Models.PrivateChat;
import uth.edu.appchat.Models.PrivateChatNickname;
import uth.edu.appchat.Models.User;
import uth.edu.appchat.Repositories.GroupChatRepository;
import uth.edu.appchat.Repositories.PrivateChatNicknameRepository;
import uth.edu.appchat.Repositories.PrivateChatRepository;
import uth.edu.appchat.Repositories.UserRepository;
import uth.edu.appchat.Models.GroupChat;

import java.time.LocalDateTime;

@Service
public class ChatNicknameService {

    private static final Logger log = LoggerFactory.getLogger(ChatNicknameService.class);

    private final GroupChatRepository groupRepo;
    private final PrivateChatRepository privateRepo;
    private final PrivateChatNicknameRepository privNickRepo;
    private final UserRepository userRepo;
    private final SimpMessagingTemplate ws;

    public ChatNicknameService(GroupChatRepository g, PrivateChatRepository p,
                               PrivateChatNicknameRepository privNickRepo, UserRepository userRepo,
                               SimpMessagingTemplate ws) {
        this.groupRepo = g;
        this.privateRepo = p;
        this.privNickRepo = privNickRepo;
        this.userRepo = userRepo;
        this.ws = ws;
    }

    /* =================== GROUP (giữ nguyên) =================== */
    @Transactional
    public NicknameResponse updateGroup(Long groupId, String raw, Long actorUserId) {
        String nickname = normalize(raw);

        GroupChat gc = groupRepo.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group không tồn tại"));

        gc.setNickname(nickname);
        gc.setNicknameUpdatedBy(actorUserId);
        gc.setNicknameUpdatedAt(LocalDateTime.now());
        GroupChat saved = groupRepo.save(gc);

        // Tiêu đề hiệu lực (nickname nếu có, không thì name gốc)
        String effectiveTitle = (saved.getNickname() != null && !saved.getNickname().isBlank())
                ? saved.getNickname()
                : saved.getName();

        // Event đơn giản cho FE
        record GroupTitleChangedEvent(String event, Long groupId, String title) {}
        GroupTitleChangedEvent evt = new GroupTitleChangedEvent("GROUP_TITLE_CHANGED", saved.getId(), effectiveTitle);

        // Broadcast tới mọi thành viên của nhóm
        ws.convertAndSend("/topic/groups/" + groupId, evt);

        // Giữ nguyên response REST (nếu FE dùng)
        return new NicknameResponse(
                saved.getId(),
                "GROUP",
                saved.getNickname(),
                saved.getNicknameUpdatedBy(),
                saved.getNicknameUpdatedAt(),
                saved.getVersion()
        );
    }




    /* ============ PRIVATE (viewer-scoped nickname) ============ */
    @Transactional
    public NicknameResponse updatePrivate(Long chatId, String raw, Long actorUserId) {
        String nickname = normalize(raw);
        LocalDateTime now = LocalDateTime.now();

        log.info("Updating nickname for chatId={}, actorUserId={}, nickname={}", chatId, actorUserId, nickname);

        // Lấy chat kèm user1/user2
        PrivateChat chat = privateRepo.findWithUsersById(chatId)
                .orElseThrow(() -> new EntityNotFoundException("Private chat không tồn tại"));
        log.info("Found chat: id={}", chat.getId());

        User owner = userRepo.findById(actorUserId)
                .orElseThrow(() -> new EntityNotFoundException("User không tồn tại"));

        if (!chat.containsUser(owner)) {
            throw new IllegalArgumentException("Bạn không thuộc cuộc chat này");
        }

        User target = chat.getOtherUser(owner);
        if (target == null) {
            throw new IllegalStateException("Không xác định được đối phương trong private chat");
        }

        // Xử lý PrivateChatNickname (chỉ cập nhật biệt danh cho target)
        PrivateChatNickname row = privNickRepo
                .findByChat_IdAndOwner_IdAndTarget_Id(chat.getId(), owner.getId(), target.getId())
                .orElse(null);

        if (nickname == null) {
            if (row != null) {
                privNickRepo.delete(row);
                log.info("Deleted PrivateChatNickname for chatId={}, ownerId={}, targetId={}", chatId, owner.getId(), target.getId());
            }
        } else {
            if (row == null) {
                row = new PrivateChatNickname();
                row.setChat(chat);
                row.setOwner(owner);
                row.setTarget(target);
            }
            row.setNickname(nickname);
            row.setUpdatedAt(now);
            PrivateChatNickname savedNick = privNickRepo.save(row);
            log.info("Saved PrivateChatNickname: id={}, nickname={}", savedNick.getId(), savedNick.getNickname());
        }

        NicknameResponse resp = new NicknameResponse(
                chat.getId(),
                "PRIVATE",
                nickname,
                owner.getId(),
                now,
                null
        );

        ws.convertAndSend("/topic/private/" + chatId + "/nickname", resp);
        log.info("Sent WebSocket update for chatId={}", chatId);
        return resp;
    }

    /* ===================== helpers ===================== */
    private String normalize(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        return s.isBlank() ? null : s;
    }
}