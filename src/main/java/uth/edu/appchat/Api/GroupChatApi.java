package uth.edu.appchat.Api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import uth.edu.appchat.Dtos.*;
import uth.edu.appchat.Models.GroupChat;
import uth.edu.appchat.Models.User;
import uth.edu.appchat.Repositories.UserRepository;
import uth.edu.appchat.Services.GroupChatService;

import java.util.ArrayList;
import java.util.List;
import java.security.Principal;
import java.util.Optional;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import uth.edu.appchat.Dtos.MessageContentDTO;
import uth.edu.appchat.Dtos.AttachmentDTO;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupChatApi {
    private final GroupChatService groupChatService;
    private final UserRepository userRepo;
    private final SimpMessagingTemplate messaging;

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
                else if (url.contains("/uploads/videos/")) mt = "VIDEO";
                else                                       mt = "TEXT";
                m.setMessageType(mt);
            }

            boolean isText = "TEXT".equalsIgnoreCase(mt) || "SYSTEM".equalsIgnoreCase(mt);
            if (!isText) {
                String url = m.getContent() == null ? "" : m.getContent();
                if (!url.isBlank()) {
                    AttachmentDTO a = new AttachmentDTO();
                    a.setType(mt.equalsIgnoreCase("IMAGE") ? "image"
                            : mt.equalsIgnoreCase("VIDEO") ? "video" : "file");
                    a.setUrl(url);
                    a.setName(url.substring(url.lastIndexOf('/') + 1));
                    a.setSize(null);
                    m.setAttachments(java.util.List.of(a));
                } else {
                    m.setAttachments(java.util.List.of());
                }
            } else {
                m.setAttachments(java.util.List.of());
            }
        }
        return ResponseEntity.ok(messages);
    }



    @PostMapping("/{groupId}/send")
    public ResponseEntity<GroupMessageDTO> sendGroupMessage(@PathVariable Long groupId,
                                                            @RequestBody MessageContentDTO contentDTO) {
        String content = Optional.ofNullable(contentDTO).map(MessageContentDTO::getContent).orElse("");
        List<AttachmentDTO> atts = Optional.ofNullable(contentDTO).map(MessageContentDTO::getAttachments).orElse(List.of());
        GroupMessageDTO message = groupChatService.sendGroupMessage(groupId, content);
        message.setAttachments(atts);
        List<MemberNicknameDTO> members = groupChatService.getMembersWithNickname(groupId);
        for (MemberNicknameDTO m : members) {
            String u = m.getUsername();
            if (u != null && !u.isBlank()) {
                messaging.convertAndSendToUser(u, "/queue/group", message);
            }
        }
        return ResponseEntity.ok(message);
    }



    @GetMapping("/{groupId}/members-with-nickname")
    public List<MemberNicknameDTO> members(@PathVariable Long groupId) {
        return groupChatService.getMembersWithNickname(groupId);
    }

    // POST /api/groups/{groupId}/nicknames
    @PostMapping("/{groupId}/nicknames")
    public ResponseEntity<Void> save(@PathVariable Long groupId,
                                     @RequestBody List<MemberNicknameDTO> payload,
                                     Principal principal) {
        groupChatService.saveMemberNicknames(groupId, principal.getName(), payload);
        return ResponseEntity.ok().build();
    }
    @PostMapping("/{groupId}/clear")
    public ResponseEntity<Void> clearForMe(@PathVariable Long groupId) {
        groupChatService.clearGroupForMe(groupId);
        return ResponseEntity.ok().build();
    }
}
