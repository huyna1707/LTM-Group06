package uth.edu.appchat.Api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import uth.edu.appchat.Dtos.*;
import uth.edu.appchat.Models.GroupChat;
import uth.edu.appchat.Models.GroupMessage;
import uth.edu.appchat.Models.User;
import uth.edu.appchat.Repositories.GroupChatRepository;
import uth.edu.appchat.Repositories.GroupMemberRepository;
import uth.edu.appchat.Repositories.GroupMessageRepository;
import uth.edu.appchat.Repositories.UserRepository;
import uth.edu.appchat.Services.GroupChatService;
import uth.edu.appchat.Dtos.AddMembersRequest;
import uth.edu.appchat.Dtos.AddMembersResult;
import java.time.Instant;
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
    private final GroupMemberRepository groupMemberRepo;   // 👈 THÊM

    private UserDTO toUserDto(User u) {
        if (u == null) return null;
        String fullName = Optional.ofNullable(u.getFullName()).orElse(u.getUsername());
        return new UserDTO(u.getId(), u.getUsername(), fullName);
    }

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

        GroupDTO groupDTO = new GroupDTO(
                group.getId(),
                group.getName(),
                group.getMemberCount(),
                group.getAvatarUrl(),
                group.getNickname()  // 👈 thêm tham số 4
        );
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
                m.setContent(""); // xoá URL khỏi content để FE không in đường dẫn
            } else {
                m.setAttachments(List.of());
            }
        }
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/{groupId}/send")
    public ResponseEntity<GroupMessageDTO> sendGroupMessage(
            @PathVariable Long groupId,
            @RequestBody MessageContentDTO body
    ) {
        // Lấy username hiện tại
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String meUsername = (auth != null) ? auth.getName() : null;

        // Chuẩn hóa input
        String content = Optional.ofNullable(body.getContent()).orElse("").trim();
        List<AttachmentDTO> atts = (body.getAttachments() == null) ? List.of() : body.getAttachments();

        // Không cho gửi rỗng (không text + không file)
        if (content.isEmpty() && atts.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Tìm group + user
        GroupChat group = groupChatRepo.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy group: " + groupId));
        User me = userRepo.findByUsername(meUsername)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user: " + meUsername));

        // Danh sách thành viên để broadcast
        List<MemberNicknameDTO> members = groupChatService.getMembersWithNickname(groupId);

        GroupMessageDTO lastDto = null;

        // 1) Lưu TEXT (nếu có) -> phát 1 event
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

        // 2) Lưu ATTACHMENTS (nếu có) -> mỗi file là 1 message, nhưng vẫn 1 lần gửi từ phía người dùng
        for (AttachmentDTO a : atts) {
            String url = Optional.ofNullable(a.getUrl()).orElse("").trim();
            if (url.isEmpty()) continue;

            // Map loại file
            String t = Optional.ofNullable(a.getType()).orElse("").toLowerCase();
            GroupMessage.MessageType mt;
            if ("image".equals(t))      mt = GroupMessage.MessageType.IMAGE;
            else /* "video" hoặc khác*/ mt = GroupMessage.MessageType.FILE; // nếu có ENUM VIDEO thì đổi sang VIDEO

            GroupMessage gm = new GroupMessage();
            gm.setGroupChat(group);
            gm.setSender(me);
            gm.setContent(url);                // content lưu URL file
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

        // Trả về message cuối cùng (hoặc tạo stub nếu chỉ có text)
        return ResponseEntity.ok(
                lastDto != null ? lastDto
                        : new GroupMessageDTO(null, groupId, toUserDto(me), content, LocalDateTime.now())
        );
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

    @PostMapping("/{groupId}/leave")
    public ResponseEntity<?> leave(@PathVariable Long groupId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        String fullName = userRepo.findByUsername(username)
                .map(u -> Optional.ofNullable(u.getFullName()).orElse(u.getUsername()))
                .orElse(username);

        // người nhận (lấy trước khi rời)
        var recipients = groupMemberRepo.findActiveUsernames(groupId);
        if (!recipients.contains(username)) recipients.add(username);

        groupChatService.leaveGroup(groupId); // rời nhóm (soft/hard tuỳ bạn đã chọn)

        var payload = new java.util.HashMap<String, Object>();
        payload.put("event", "GROUP_MEMBER_LEFT");
        payload.put("groupId", groupId);
        payload.put("username", username);
        payload.put("fullName", fullName);
        payload.put("timestamp", java.time.Instant.now().toString());

        for (String u : recipients) {
            messaging.convertAndSendToUser(u, "/queue/group", payload);
        }
        return ResponseEntity.ok(java.util.Map.of("success", true));
    }


    @DeleteMapping("/{groupId}")
    public ResponseEntity<?> delete(@PathVariable Long groupId) {
        String byUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        GroupChat group = groupChatRepo.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhóm."));
        String groupName = group.getName();

        // lấy danh sách người nhận TRƯỚC khi xóa
        var recipients = groupMemberRepo.findActiveUsernames(groupId);
        if (!recipients.contains(byUsername)) recipients.add(byUsername);

        groupChatService.deleteGroup(groupId); // xóa nhóm (đã check quyền trong service)

        var payload = new java.util.HashMap<String, Object>();
        payload.put("event", "GROUP_DELETED");
        payload.put("groupId", groupId);
        payload.put("groupName", groupName);
        payload.put("by", byUsername);
        payload.put("timestamp", java.time.Instant.now().toString());

        for (String u : recipients) {
            messaging.convertAndSendToUser(u, "/queue/group", payload);
        }
        return ResponseEntity.ok(java.util.Map.of("success", true));
    }



    @GetMapping("/{groupId}/me")
    public ResponseEntity<GroupMembershipDTO> myMembership(@PathVariable Long groupId) {
        return ResponseEntity.ok(groupChatService.getMyMembership(groupId));
    }
    @PostMapping("/{groupId}/members")
    public ResponseEntity<?> addMembers(
            @PathVariable Long groupId,
            @RequestBody AddMembersRequest req
    ) {
        try {
            AddMembersResult r = groupChatService.addMembers(groupId, req);
            return ResponseEntity.ok(java.util.Map.of(
                    "success", true,
                    "result", r
            ));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(403).body(java.util.Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(java.util.Map.of(
                    "success", false,
                    "message", "Lỗi hệ thống, vui lòng thử lại."
            ));
        }
    }
    @PatchMapping("/{groupId}/avatar")
    public ResponseEntity<?> setAvatar(@PathVariable Long groupId, @RequestBody java.util.Map<String,String> body) {
        String by = SecurityContextHolder.getContext().getAuthentication().getName();
        String url = java.util.Optional.ofNullable(body.get("url")).orElse("");
        groupChatService.setGroupAvatar(groupId, by, url);

        var recipients = groupMemberRepo.findActiveUsernames(groupId);
        var payload = new java.util.HashMap<String,Object>();
        payload.put("event", "GROUP_AVATAR_CHANGED");
        payload.put("groupId", groupId);
        payload.put("avatarUrl", url);
        payload.put("by", by);
        payload.put("timestamp", java.time.Instant.now().toString());
        for (String u : recipients) {
            messaging.convertAndSendToUser(u, "/queue/group", payload);
        }
        return ResponseEntity.ok(java.util.Map.of("success", true, "url", url));
    }

    @DeleteMapping("/{groupId}/avatar")
    public ResponseEntity<?> clearAvatar(@PathVariable Long groupId) {
        String by = SecurityContextHolder.getContext().getAuthentication().getName();

        // gọi trực tiếp setGroupAvatar với url = null
        groupChatService.setGroupAvatar(groupId, by, null);

        var recipients = groupMemberRepo.findActiveUsernames(groupId);
        var payload = new java.util.HashMap<String,Object>();
        payload.put("event", "GROUP_AVATAR_CHANGED");
        payload.put("groupId", groupId);
        payload.put("avatarUrl", "");
        payload.put("by", by);
        payload.put("timestamp", java.time.Instant.now().toString());
        for (String u : recipients) {
            messaging.convertAndSendToUser(u, "/queue/group", payload);
        }
        return ResponseEntity.ok(java.util.Map.of("success", true));
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<?> kickMember(@PathVariable Long groupId, @PathVariable Long userId) {
        String by = SecurityContextHolder.getContext().getAuthentication().getName();
        var recipients = groupMemberRepo.findActiveUsernames(groupId);
        userRepo.findById(userId).map(User::getUsername).ifPresent(recipients::add);

        groupChatService.kickMember(groupId, userId, by);

        long active = groupMemberRepo.countByGroupChatIdAndIsActiveTrue(groupId);
        var payload = new java.util.HashMap<String,Object>();
        payload.put("event", "GROUP_MEMBER_KICKED");
        payload.put("groupId", groupId);
        payload.put("username", userRepo.findById(userId).map(User::getUsername).orElse(null));
        payload.put("activeCount", active);
        payload.put("by", by);
        payload.put("timestamp", java.time.Instant.now().toString());
        for (String u : recipients) messaging.convertAndSendToUser(u, "/queue/group", payload);

        return ResponseEntity.ok(java.util.Map.of("success", true));
    }
    @PostMapping("/{groupId}/kick/{userId}")
    public ResponseEntity<?> kick(@PathVariable Long groupId, @PathVariable Long userId) {
        String by = SecurityContextHolder.getContext().getAuthentication().getName();

        groupChatService.kickMember(groupId, userId, by);

        var recipients = groupMemberRepo.findActiveUsernames(groupId);
        var payload = new java.util.HashMap<String,Object>();
        payload.put("event", "GROUP_MEMBER_KICKED");
        payload.put("groupId", groupId);
        payload.put("kickedUserId", userId);
        payload.put("by", by);
        payload.put("activeCount", groupMemberRepo.countByGroupChatIdAndIsActiveTrue(groupId));
        payload.put("timestamp", java.time.Instant.now().toString());
        for (String u : recipients) {
            messaging.convertAndSendToUser(u, "/queue/group", payload);
        }

        return ResponseEntity.ok(java.util.Map.of("success", true));
    }
}

