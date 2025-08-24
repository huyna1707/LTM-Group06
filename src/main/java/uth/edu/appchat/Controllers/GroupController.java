//package uth.edu.appchat.Controllers;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.web.bind.annotation.*;
//import uth.edu.appchat.Models.*;
//import uth.edu.appchat.Repositories.*;
//
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//
//@RestController
//@RequestMapping("/api/groups")
//@RequiredArgsConstructor
//public class GroupController {
//
//    private final GroupChatRepository groupChatRepository;
//    private final GroupMemberRepository groupMemberRepository;
//    private final GroupMessageRepository groupMessageRepository;
//    private final UserRepository userRepository;
//    private final SimpMessagingTemplate messagingTemplate;
//
//    // DTO classes
//    public static class CreateGroupDto {
//        public String name;
//        public String description;
//    }
//
//    public static class SendMessageDto {
//        public String content;
//    }
//
//    public static class GroupDto {
//        public Long id;
//        public String name;
//        public String description;
//        public String createdBy;
//        public LocalDateTime createdAt;
//        public int memberCount;
//        public LocalDateTime lastMessageAt;
//    }
//
//    public static class GroupMessageDto {
//        public Long id;
//        public String content;
//        public SenderDto sender;
//        public LocalDateTime timestamp;
//        public String messageType;
//    }
//
//    public static class SenderDto {
//        public String username;
//        public String fullName;
//    }
//
//    // Tạo nhóm chat mới
//    @PostMapping("/create")
//    public ResponseEntity<?> createGroup(
//            @RequestBody CreateGroupDto request,
//            @AuthenticationPrincipal UserDetails userDetails) {
//
//        try {
//            User currentUser = userRepository.findByUsername(userDetails.getUsername())
//                    .orElse(null);
//            if (currentUser == null) {
//                return ResponseEntity.badRequest().body("Không tìm thấy người dùng hiện tại");
//            }
//
//            if (request.name == null || request.name.trim().isEmpty()) {
//                return ResponseEntity.badRequest().body("Tên nhóm không được để trống");
//            }
//
//            // Tạo nhóm chat mới
//            GroupChat group = new GroupChat();
//            group.setName(request.name.trim());
//            group.setDescription(request.description != null ? request.description.trim() : "");
//            group.setCreatedBy(currentUser);
//            group.setCreatedAt(LocalDateTime.now());
//
//            group = groupChatRepository.save(group);
//
//            // Thêm người tạo làm admin của nhóm
//            GroupMember creator = new GroupMember();
//            creator.setGroupChat(group);
//            creator.setUser(currentUser);
//            creator.setRole(GroupMember.GroupRole.ADMIN);
//            creator.setJoinedAt(LocalDateTime.now());
//            creator.setIsActive(true);
//
//            groupMemberRepository.save(creator);
//
//            // Trả về thông tin nhóm vừa tạo
//            GroupDto groupDto = new GroupDto();
//            groupDto.id = group.getId();
//            groupDto.name = group.getName();
//            groupDto.description = group.getDescription();
//            groupDto.createdBy = currentUser.getFullName() != null ? currentUser.getFullName() : currentUser.getUsername();
//            groupDto.createdAt = group.getCreatedAt();
//            groupDto.memberCount = 1;
//            groupDto.lastMessageAt = null;
//
//            return ResponseEntity.ok(groupDto);
//
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body("Có lỗi xảy ra khi tạo nhóm: " + e.getMessage());
//        }
//    }
//
//    // Lấy danh sách nhóm chat của user
//    @GetMapping("/my-groups")
//    public ResponseEntity<List<GroupDto>> getMyGroups(@AuthenticationPrincipal UserDetails userDetails) {
//        try {
//            User currentUser = userRepository.findByUsername(userDetails.getUsername())
//                    .orElse(null);
//            if (currentUser == null) {
//                return ResponseEntity.badRequest().build();
//            }
//
//            List<GroupMember> memberships = groupMemberRepository.findByUserAndIsActiveTrue(currentUser);
//            List<GroupDto> groups = new ArrayList<>();
//
//            for (GroupMember membership : memberships) {
//                GroupChat group = membership.getGroupChat();
//
//                GroupDto groupDto = new GroupDto();
//                groupDto.id = group.getId();
//                groupDto.name = group.getName();
//                groupDto.description = group.getDescription();
//                groupDto.createdBy = group.getCreatedBy().getFullName() != null
//                        ? group.getCreatedBy().getFullName()
//                        : group.getCreatedBy().getUsername();
//                groupDto.createdAt = group.getCreatedAt();
//                groupDto.memberCount = group.getMemberCount();
//                groupDto.lastMessageAt = group.getLastMessageAt();
//
//                groups.add(groupDto);
//            }
//
//            return ResponseEntity.ok(groups);
//
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().build();
//        }
//    }
//
//    // Lấy tin nhắn của nhóm
//    @GetMapping("/{groupId}/messages")
//    public ResponseEntity<List<GroupMessageDto>> getGroupMessages(
//            @PathVariable Long groupId,
//            @AuthenticationPrincipal UserDetails userDetails) {
//
//        try {
//            User currentUser = userRepository.findByUsername(userDetails.getUsername())
//                    .orElse(null);
//            if (currentUser == null) {
//                return ResponseEntity.badRequest().build();
//            }
//
//            GroupChat group = groupChatRepository.findById(groupId).orElse(null);
//            if (group == null) {
//                return ResponseEntity.badRequest().build();
//            }
//
//            // Kiểm tra xem user có phải thành viên của nhóm không
//            Optional<GroupMember> membership = groupMemberRepository.findByGroupChatAndUser(group, currentUser);
//            if (membership.isEmpty()) {
//                return ResponseEntity.status(403).build(); // Forbidden
//            }
//
//            List<GroupMessage> messages = groupMessageRepository.findByGroupChatOrderByCreatedAtAsc(group);
//            List<GroupMessageDto> messageDtos = new ArrayList<>();
//
//            for (GroupMessage message : messages) {
//                GroupMessageDto messageDto = new GroupMessageDto();
//                messageDto.id = message.getId();
//                messageDto.content = message.getContent();
//                messageDto.timestamp = message.getCreatedAt();
//                messageDto.messageType = message.getMessageType().toString();
//
//                SenderDto senderDto = new SenderDto();
//                senderDto.username = message.getSender().getUsername();
//                senderDto.fullName = message.getSender().getFullName() != null
//                        ? message.getSender().getFullName()
//                        : message.getSender().getUsername();
//                messageDto.sender = senderDto;
//
//                messageDtos.add(messageDto);
//            }
//
//            return ResponseEntity.ok(messageDtos);
//
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().build();
//        }
//    }
//
//    // Gửi tin nhắn trong nhóm
//    @PostMapping("/{groupId}/send")
//    public ResponseEntity<?> sendGroupMessage(
//            @PathVariable Long groupId,
//            @RequestBody SendMessageDto request,
//            @AuthenticationPrincipal UserDetails userDetails) {
//
//        try {
//            User currentUser = userRepository.findByUsername(userDetails.getUsername())
//                    .orElse(null);
//            if (currentUser == null) {
//                return ResponseEntity.badRequest().body("Không tìm thấy người dùng hiện tại");
//            }
//
//            if (request.content == null || request.content.trim().isEmpty()) {
//                return ResponseEntity.badRequest().body("Nội dung tin nhắn không được để trống");
//            }
//
//            GroupChat group = groupChatRepository.findById(groupId).orElse(null);
//            if (group == null) {
//                return ResponseEntity.badRequest().body("Không tìm thấy nhóm chat");
//            }
//
//            // Kiểm tra xem user có phải thành viên của nhóm không
//            Optional<GroupMember> membership = groupMemberRepository.findByGroupChatAndUser(group, currentUser);
//            if (membership.isEmpty()) {
//                return ResponseEntity.status(403).body("Bạn không phải thành viên của nhóm này");
//            }
//
//            // Tạo tin nhắn mới
//            GroupMessage message = new GroupMessage();
//            message.setGroupChat(group);
//            message.setSender(currentUser);
//            message.setContent(request.content.trim());
//            message.setMessageType(GroupMessage.MessageType.TEXT);
//            message.setCreatedAt(LocalDateTime.now());
//
//            message = groupMessageRepository.save(message);
//
//            // Cập nhật lastMessageAt cho nhóm
//            group.setLastMessageAt(LocalDateTime.now());
//            groupChatRepository.save(group);
//
//            // Gửi tin nhắn real-time tới tất cả thành viên nhóm
//            GroupMessageDto messageDto = new GroupMessageDto();
//            messageDto.id = message.getId();
//            messageDto.content = message.getContent();
//            messageDto.timestamp = message.getCreatedAt();
//            messageDto.messageType = message.getMessageType().toString();
//
//            SenderDto senderDto = new SenderDto();
//            senderDto.username = currentUser.getUsername();
//            senderDto.fullName = currentUser.getFullName() != null
//                    ? currentUser.getFullName()
//                    : currentUser.getUsername();
//            messageDto.sender = senderDto;
//
//            // Gửi tới tất cả thành viên nhóm
//            List<GroupMember> members = groupMemberRepository.findByGroupChatAndIsActiveTrue(group);
//            for (GroupMember member : members) {
//                Map<String, Object> notification = new HashMap<>();
//                notification.put("type", "GROUP_MESSAGE");
//                notification.put("groupId", groupId);
//                notification.put("message", messageDto);
//
//                messagingTemplate.convertAndSendToUser(
//                        member.getUser().getUsername(),
//                        "/group",
//                        notification
//                );
//            }
//
//            return ResponseEntity.ok(messageDto);
//
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body("Có lỗi xảy ra khi gửi tin nhắn: " + e.getMessage());
//        }
//    }
//
//    // Thêm thành viên vào nhóm
//    @PostMapping("/{groupId}/add-member")
//    public ResponseEntity<?> addMemberToGroup(
//            @PathVariable Long groupId,
//            @RequestBody Map<String, String> request,
//            @AuthenticationPrincipal UserDetails userDetails) {
//
//        try {
//            User currentUser = userRepository.findByUsername(userDetails.getUsername())
//                    .orElse(null);
//            if (currentUser == null) {
//                return ResponseEntity.badRequest().body("Không tìm thấy người dùng hiện tại");
//            }
//
//            String targetUsername = request.get("username");
//            if (targetUsername == null || targetUsername.trim().isEmpty()) {
//                return ResponseEntity.badRequest().body("Tên người dùng không được để trống");
//            }
//
//            GroupChat group = groupChatRepository.findById(groupId).orElse(null);
//            if (group == null) {
//                return ResponseEntity.badRequest().body("Không tìm thấy nhóm chat");
//            }
//
//            // Kiểm tra quyền admin
//            Optional<GroupMember> currentUserMembership = groupMemberRepository.findByGroupChatAndUser(group, currentUser);
//            if (currentUserMembership.isEmpty() || currentUserMembership.get().getRole() != GroupMember.GroupRole.ADMIN) {
//                return ResponseEntity.status(403).body("Chỉ admin mới có thể thêm thành viên");
//            }
//
//            User targetUser = userRepository.findByUsername(targetUsername).orElse(null);
//            if (targetUser == null) {
//                return ResponseEntity.badRequest().body("Không tìm thấy người dùng: " + targetUsername);
//            }
//
//            // Kiểm tra xem user đã là thành viên chưa
//            Optional<GroupMember> existingMember = groupMemberRepository.findByGroupChatAndUser(group, targetUser);
//            if (existingMember.isPresent()) {
//                return ResponseEntity.badRequest().body("Người dùng đã là thành viên của nhóm");
//            }
//
//            // Thêm thành viên mới
//            GroupMember newMember = new GroupMember();
//            newMember.setGroupChat(group);
//            newMember.setUser(targetUser);
//            newMember.setRole(GroupMember.GroupRole.MEMBER);
//            newMember.setJoinedAt(LocalDateTime.now());
//            newMember.setIsActive(true);
//
//            groupMemberRepository.save(newMember);
//
//            return ResponseEntity.ok("Đã thêm thành viên thành công");
//
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body("Có lỗi xảy ra: " + e.getMessage());
//        }
//    }
//}