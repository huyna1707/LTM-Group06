package uth.edu.appchat.Api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import uth.edu.appchat.Dtos.CreateGroupForm;
import uth.edu.appchat.Dtos.GroupDTO;
import uth.edu.appchat.Dtos.GroupMessageDTO;
import uth.edu.appchat.Dtos.MessageContentDTO;
import uth.edu.appchat.Models.GroupChat;
import uth.edu.appchat.Models.User;
import uth.edu.appchat.Repositories.UserRepository;
import uth.edu.appchat.Services.GroupChatService;
import java.util.List;
import uth.edu.appchat.Dtos.MemberNicknameDTO;
import java.security.Principal;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupChatApi {
    private final GroupChatService groupChatService;
    private final UserRepository userRepo;

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
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/{groupId}/send")
    public ResponseEntity<GroupMessageDTO> sendGroupMessage(@PathVariable Long groupId, @RequestBody MessageContentDTO contentDTO) {
        GroupMessageDTO message = groupChatService.sendGroupMessage(groupId, contentDTO.getContent());
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
