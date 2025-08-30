package uth.edu.appchat.Api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import uth.edu.appchat.Dtos.AttachmentDTO;
import uth.edu.appchat.Dtos.CreateGroupForm;
import uth.edu.appchat.Dtos.GroupDTO;
import uth.edu.appchat.Dtos.GroupMessageDTO;
import uth.edu.appchat.Dtos.MemberNicknameDTO;
import uth.edu.appchat.Dtos.MessageContentDTO;
import uth.edu.appchat.Dtos.UserDTO;
import uth.edu.appchat.Models.GroupChat;
import uth.edu.appchat.Models.GroupMessage;
import uth.edu.appchat.Models.User;
import uth.edu.appchat.Repositories.GroupChatRepository;
import uth.edu.appchat.Repositories.GroupMessageRepository;
import uth.edu.appchat.Repositories.UserRepository;
import uth.edu.appchat.Services.GroupChatService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupChatApi {

    private final GroupChatService groupChatService;
    private final UserRepository userRepo;
    private final SimpMessagingTemplate messaging;
    private final GroupChatRepository groupChatRepo;
    private final GroupMessageRepository groupMessageRepo;

    // ===== Helpers ============================================================

    private UserDTO toUserDto(User u) {
        if (u == null) return null;
        String fullName = Optional.ofNullable(u.getFullName()).orElse(u.getUsername());
        return new UserDTO(u.getId(), u.getUsername(), fullName);
    }

    /** Convert entity -> DTO CHUẨN khớp format GET:
     *  - TEXT:  content có, attachments rỗng
     *  - IMAGE/FILE: content = "", attachments = [{url,type,name}]
     */
    private GroupMessageDTO toDto(GroupMessage gm) {
        UserDTO sender = toUserDto(gm.getSender());
        String content = (gm.getMessageType() == GroupMessage.MessageType.TEXT)
                ? Optional.ofNullable(gm.getContent()).orElse("")
                : "";

        GroupMessageDTO dto = new GroupMessageDTO(
                gm.getId(),
                gm.getGroupChat().getId(),
                sender,
                content,
                gm.getCreatedAt()
        );
        dto.setMessageType(gm.getMessageType().toString());

        if (gm.getMessageType() == GroupMessage.MessageType.TEXT) {
            dto.setAttachments(List.of());
        } else {
            String url = Optional.ofNullable(gm.getContent()).orElse("");
            String type = (gm.getMessageType() == GroupMessage.MessageType.IMAGE) ? "image" : "file"; // không dùng VIDEO
            AttachmentDTO att = new AttachmentDTO();
            att.setType(type);
            att.setUrl(url);
            att.setName(url.lastIndexOf('/') >= 0 ? url.substring(url.lastIndexOf('/') + 1) : url);
            att.setSize(null);
            dto.setAttachments(List.of(att));
        }
        return dto;
    }
    @GetMapping("/my-groups")
    public ResponseEntity<List<GroupDTO>> getMyGroups() {
        List<GroupDTO> groups = groupChatService.getMyGroups();
        return ResponseEntity.ok(groups);
    }

    @PostMapping("/create")
    public ResponseEntity<GroupDTO> createGroup(@RequestBody CreateGroupForm form) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User creator = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + username));
        GroupChat group = groupChatService.createGroup(form, creator);
        GroupDTO groupDTO = new GroupDTO(group.getId(), group.getName(), group.getMemberCount());
        return ResponseEntity.ok(groupDTO);
    }

    @GetMapping("/{groupId}/messages")
    public ResponseEntity<List<GroupMessageDTO>> getGroupMessages(@PathVariable Long groupId) {
        List<GroupMessageDTO> messages = groupChatService.getGroupMessages(groupId);

        for (GroupMessageDTO m : messages) {
            String mt = m.getMessageType();
            if (mt == null || mt.isBlank()) {
                String url = m.getContent() == null ? "" : m.getContent();
                if      (url.contains("/uploads/images/")) mt = "IMAGE";
                else if (url.contains("/uploads/files/"))  mt = "FILE";
                else                                       mt = "TEXT";
                m.setMessageType(mt);
            }

            boolean isText = "TEXT".equalsIgnoreCase(mt) || "SYSTEM".equalsIgnoreCase(mt);
            if (!isText) {
                String url = Optional.ofNullable(m.getContent()).orElse("");
                if (!url.isBlank()) {
                    AttachmentDTO a = new AttachmentDTO();
                    a.setType(mt.equalsIgnoreCase("IMAGE") ? "image" : "file"); // không dùng video
                    a.setUrl(url);
                    a.setName(url.substring(url.lastIndexOf('/') + 1));
                    a.setSize(null);
                    m.setAttachments(List.of(a));
                } else {
                    m.setAttachments(List.of());
                }
                m.setContent("");
            } else {
                m.setAttachments(List.of());
            }
        }
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/{groupId}/send")
    public ResponseEntity<GroupMessageDTO> sendGroupMessage(@PathVariable Long groupId,
                                                            @RequestBody MessageContentDTO contentDTO) {
        String content = Optional.ofNullable(contentDTO).map(MessageContentDTO::getContent).orElse("").trim();
        List<AttachmentDTO> atts = Optional.ofNullable(contentDTO).map(MessageContentDTO::getAttachments).orElse(List.of());
        if (content.isBlank() && atts.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String meUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User me = userRepo.findByUsername(meUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));
        GroupChat group = groupChatRepo.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        List<MemberNicknameDTO> members = groupChatService.getMembersWithNickname(groupId);
        GroupMessageDTO lastDto = null;
        if (!content.isBlank()) {
            GroupMessage text = new GroupMessage();
            text.setGroupChat(group);
            text.setSender(me);
            text.setContent(content);
            text.setMessageType(GroupMessage.MessageType.TEXT);
            text.setCreatedAt(LocalDateTime.now());
            groupMessageRepo.save(text);

            GroupMessageDTO dto = toDto(text);
            lastDto = dto;

            for (MemberNicknameDTO m : members) {
                String u = m.getUsername();
                if (u != null && !u.isBlank()) {
                    messaging.convertAndSendToUser(u, "/queue/group", dto);
                }
            }
        }
        for (AttachmentDTO a : atts) {
            String url = Optional.ofNullable(a.getUrl()).orElse("").trim();
            if (url.isEmpty()) continue;
            GroupMessage.MessageType mt;
            String t = Optional.ofNullable(a.getType()).orElse("").toLowerCase();
            if ("image".equals(t)) mt = GroupMessage.MessageType.IMAGE;
            else                   mt = GroupMessage.MessageType.FILE;

            GroupMessage gm = new GroupMessage();
            gm.setGroupChat(group);
            gm.setSender(me);
            gm.setContent(url);
            gm.setMessageType(mt);
            gm.setCreatedAt(LocalDateTime.now());
            groupMessageRepo.save(gm);

            GroupMessageDTO dto = toDto(gm);
            lastDto = dto;

            for (MemberNicknameDTO m : members) {
                String u = m.getUsername();
                if (u != null && !u.isBlank()) {
                    messaging.convertAndSendToUser(u, "/queue/group", dto);
                }
            }
        }

        // trả về message cuối cùng đã gửi (nếu FE cần)
        return ResponseEntity.ok(lastDto != null ? lastDto : new GroupMessageDTO(
                null, groupId, toUserDto(me), content, LocalDateTime.now()
        ));
    }

    @GetMapping("/{groupId}/members-with-nickname")
    public List<MemberNicknameDTO> members(@PathVariable Long groupId) {
        return groupChatService.getMembersWithNickname(groupId);
    }

    @PostMapping("/{groupId}/nicknames")
    public ResponseEntity<Void> save(@PathVariable Long groupId,
                                     @RequestBody List<MemberNicknameDTO> payload,
                                     java.security.Principal principal) {
        groupChatService.saveMemberNicknames(groupId, principal.getName(), payload);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{groupId}/clear")
    public ResponseEntity<Void> clearForMe(@PathVariable Long groupId) {
        groupChatService.clearGroupForMe(groupId);
        return ResponseEntity.ok().build();
    }
}
