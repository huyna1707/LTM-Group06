package uth.edu.appchat.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import uth.edu.appchat.Dtos.ChatMessage;
import uth.edu.appchat.Models.ChatStreak;
import uth.edu.appchat.Models.GroupChat;
import uth.edu.appchat.Models.GroupMessage;
import uth.edu.appchat.Models.User;
import uth.edu.appchat.Repositories.GroupChatRepository;
import uth.edu.appchat.Repositories.GroupMessageRepository;
import uth.edu.appchat.Repositories.UserRepository;
import uth.edu.appchat.Services.ChatStreakService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ChatController {
    private final SimpMessagingTemplate messaging;
    private final GroupMessageRepository groupMessageRepository;
    private final GroupChatRepository groupChatRepository;
    private final UserRepository userRepository;
    private final ChatStreakService chatStreakService;
    // Lấy hoặc tạo GroupChat "public"
    private GroupChat getOrCreatePublicChat() {
        return groupChatRepository.findByName("public").orElseGet(() -> {
            // Tìm user đầu tiên làm creator cho public chat, hoặc tạo user system
            User systemUser = userRepository.findByUsername("system")
                    .orElseGet(() -> {
                        // Nếu không có user system, lấy user đầu tiên trong database
                        return userRepository.findAll().stream()
                                .findFirst()
                                .orElse(null);
                    });

            if (systemUser == null) {
                throw new RuntimeException("No users found in database. Cannot create public chat.");
            }

            GroupChat publicChat = new GroupChat();
            publicChat.setName("public");
            publicChat.setDescription("Kênh chat công khai");
            publicChat.setCreatedBy(systemUser);
            publicChat.setCreatedAt(LocalDateTime.now());
            publicChat.setLastMessageAt(LocalDateTime.now());
            return groupChatRepository.save(publicChat);
        });
    }

    @MessageMapping("/chat.send")
    public void sendPublic(@Payload ChatMessage msg) {
        try {
            // Tìm user và group chat chung
            User sender = userRepository.findByUsername(msg.getSender()).orElse(null);
            GroupChat publicChat = getOrCreatePublicChat();

            if (sender != null) {
                // Lưu tin nhắn vào database
                GroupMessage groupMessage = new GroupMessage();
                groupMessage.setGroupChat(publicChat);
                groupMessage.setSender(sender);
                groupMessage.setContent(msg.getContent());
                groupMessage.setMessageType(GroupMessage.MessageType.TEXT);
                groupMessage.setCreatedAt(LocalDateTime.now());
                groupMessageRepository.save(groupMessage);

                // Cập nhật streak cho group chat
                ChatStreak streak = chatStreakService.updateGroupChatStreak(publicChat);
                
                // Set fullName for display
                msg.setFullName(sender.getFullName());
                
                // Thêm streak display vào tên nếu streak >= 3
                if (streak.getCurrentStreak() >= 3) {
                    String streakDisplay = streak.getStreakDisplay();
                    msg.setFullName(msg.getFullName() + " " + streakDisplay);
                }
            }

            // Set timestamp và gửi qua WebSocket
            msg.setType(ChatMessage.Type.CHAT);
            if (msg.getTimestamp() == null || msg.getTimestamp().isEmpty()) {
                msg.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
            }

            messaging.convertAndSend("/topic/public", msg);
        } catch (Exception e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
    }

    @MessageMapping("/chat.join")
    public void join(@Payload ChatMessage msg) {
        try {
            // Tìm user và group chat chung
            User sender = userRepository.findByUsername(msg.getSender()).orElse(null);
            GroupChat publicChat = getOrCreatePublicChat();

            if (sender != null) {
                // Lưu sự kiện JOIN
                GroupMessage joinMessage = new GroupMessage();
                joinMessage.setGroupChat(publicChat);
                joinMessage.setSender(sender);
                joinMessage.setContent("đã tham gia phòng chat");
                joinMessage.setMessageType(GroupMessage.MessageType.SYSTEM);
                joinMessage.setCreatedAt(LocalDateTime.now());
                groupMessageRepository.save(joinMessage);

                // Set fullName for display
                msg.setFullName(sender.getFullName());
                
                // Lấy streak display cho group chat và thêm vào tên nếu có
                String streakDisplay = chatStreakService.getGroupChatStreakDisplay(publicChat);
                if (!streakDisplay.isEmpty()) {
                    msg.setFullName(msg.getFullName() + " " + streakDisplay);
                }
            }

            msg.setType(ChatMessage.Type.JOIN);
            messaging.convertAndSend("/topic/public", msg);
        } catch (Exception e) {
            System.err.println("Error on user join: " + e.getMessage());
        }
    }

    @MessageMapping("/user.status")
    public void updateUserStatus(@Payload ChatMessage msg) {
        try {
            User user = userRepository.findByUsername(msg.getSender()).orElse(null);
            if (user != null) {
                // Cập nhật trạng thái trong database
                try {
                    User.UserStatus status = User.UserStatus.valueOf(msg.getContent().toUpperCase());
                    user.setStatus(status);
                    userRepository.save(user);
                } catch (IllegalArgumentException e) {
                    // Giữ nguyên status hiện tại nếu không hợp lệ
                }

                // Broadcast thay đổi trạng thái tới tất cả clients
                messaging.convertAndSend("/topic/user-status", msg);
            }
        } catch (Exception e) {
            System.err.println("Error updating user status: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @GetMapping("/api/messages/public")
    @ResponseBody
    public ResponseEntity<List<ChatMessage>> getPublicMessages() {
        try {
            GroupChat publicChat = getOrCreatePublicChat();

            List<GroupMessage> recentMessages = groupMessageRepository
                    .findByGroupChatOrderByCreatedAtDesc(publicChat)
                    .stream()
                    .limit(50)
                    .collect(Collectors.toList());

            // Lấy streak display cho group chat
            String groupStreakDisplay = chatStreakService.getGroupChatStreakDisplay(publicChat);

            List<ChatMessage> messages = recentMessages.stream()
                    .map(msg -> {
                        ChatMessage.Type type = msg.getMessageType() == GroupMessage.MessageType.SYSTEM
                                ? ChatMessage.Type.JOIN
                                : ChatMessage.Type.CHAT;

                        String fullName = msg.getSender().getFullName() != null ? 
                                msg.getSender().getFullName() : msg.getSender().getUsername();
                        
                        // Thêm streak display vào tên nếu có
                        if (!groupStreakDisplay.isEmpty() && type == ChatMessage.Type.CHAT) {
                            fullName = fullName + " " + groupStreakDisplay;
                        }

                        return ChatMessage.builder()
                                .sender(msg.getSender().getUsername())
                                .fullName(fullName)
                                .content(msg.getContent())
                                .type(type)
                                .timestamp(msg.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")))
                                .build();
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            System.err.println("Error getting messages: " + e.getMessage());
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/api/users/online")
    @ResponseBody
    public ResponseEntity<List<User>> getOnlineUsers(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            List<User> users = userRepository.findAll().stream()
                    .limit(20)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(users);
        } catch (Exception e) {
            System.err.println("Error getting online users: " + e.getMessage());
            return ResponseEntity.ok(List.of());
        }
    }
}