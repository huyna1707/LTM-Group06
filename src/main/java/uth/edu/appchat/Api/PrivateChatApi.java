package uth.edu.appchat.Api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import uth.edu.appchat.Models.PrivateChat;
import uth.edu.appchat.Models.PrivateMessage;
import uth.edu.appchat.Models.User;
import uth.edu.appchat.Repositories.PrivateChatRepository;
import uth.edu.appchat.Repositories.PrivateMessageRepository;
import uth.edu.appchat.Repositories.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/private-chat")
@RequiredArgsConstructor
public class PrivateChatApi {

    private final PrivateChatRepository privateChatRepository;
    private final PrivateMessageRepository privateMessageRepository;
    private final UserRepository userRepository;

    // Lấy danh sách chat riêng của user hiện tại
    @GetMapping("/my-chats")
    public ResponseEntity<?> getMyPrivateChats(Authentication auth) {
        try {
            User currentUser = userRepository.findByUsername(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<PrivateChat> chats = privateChatRepository.findByUser(currentUser);

            List<Map<String, Object>> chatList = chats.stream().map(chat -> {
                User otherUser = chat.getOtherUser(currentUser);
                return Map.of(
                        "chatId", chat.getId(),
                        "otherUser", Map.of(
                                "username", otherUser.getUsername(),
                                "fullName", otherUser.getFullName() != null ? otherUser.getFullName() : otherUser.getUsername(),
                                "status", otherUser.getStatus() != null ? otherUser.getStatus().toString() : "ONLINE"
                        ),
                        "lastMessageAt", chat.getLastMessageAt() != null ? chat.getLastMessageAt().toString() : "",
                        "createdAt", chat.getCreatedAt().toString()
                );
            }).collect(Collectors.toList());

            return ResponseEntity.ok(chatList);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to get private chats: " + e.getMessage()
            ));
        }
    }

    // Tạo hoặc lấy chat riêng với user khác
    @PostMapping("/start-chat")
    public ResponseEntity<?> startPrivateChat(@RequestBody Map<String, String> request, Authentication auth) {
        try {
            String targetUsername = request.get("username");
            if (targetUsername == null || targetUsername.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
            }

            User currentUser = userRepository.findByUsername(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Current user not found"));

            User targetUser = userRepository.findByUsername(targetUsername)
                    .orElseThrow(() -> new RuntimeException("Target user not found"));

            if (currentUser.getId().equals(targetUser.getId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Cannot chat with yourself"));
            }

            // Tìm chat hiện có hoặc tạo mới
            PrivateChat chat = privateChatRepository.findByUsers(currentUser, targetUser)
                    .orElseGet(() -> {
                        PrivateChat newChat = new PrivateChat();
                        newChat.setUser1(currentUser);
                        newChat.setUser2(targetUser);
                        newChat.setCreatedAt(LocalDateTime.now());
                        return privateChatRepository.save(newChat);
                    });

            return ResponseEntity.ok(Map.of(
                    "chatId", chat.getId(),
                    "otherUser", Map.of(
                            "username", targetUser.getUsername(),
                            "fullName", targetUser.getFullName() != null ? targetUser.getFullName() : targetUser.getUsername(),
                            "status", targetUser.getStatus() != null ? targetUser.getStatus().toString() : "ONLINE"
                    ),
                    "createdAt", chat.getCreatedAt().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to start private chat: " + e.getMessage()
            ));
        }
    }

    // Lấy tin nhắn của chat riêng
    @GetMapping("/{chatId}/messages")
    public ResponseEntity<?> getPrivateChatMessages(@PathVariable Long chatId, Authentication auth) {
        try {
            User currentUser = userRepository.findByUsername(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            PrivateChat chat = privateChatRepository.findById(chatId)
                    .orElseThrow(() -> new RuntimeException("Chat not found"));

            // Kiểm tra quyền truy cập
            if (!chat.containsUser(currentUser)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied"));
            }

            List<PrivateMessage> messages = privateMessageRepository.findByPrivateChatOrderByCreatedAtAsc(chat);

            List<Map<String, Object>> messageList = messages.stream().map(msg -> {
                User sender = msg.getSender();
                return Map.of(
                        "id", msg.getId(),
                        "content", msg.getContent(),
                        "sender", Map.of(
                                "username", sender.getUsername(),
                                "fullName", sender.getFullName() != null ? sender.getFullName() : sender.getUsername()
                        ),
                        "timestamp", msg.getCreatedAt().toString(),
                        "messageType", msg.getMessageType().toString(),
                        "isRead", msg.getIsRead() != null ? msg.getIsRead() : false
                );
            }).collect(Collectors.toList());

            return ResponseEntity.ok(messageList);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to get messages: " + e.getMessage()
            ));
        }
    }

    // Gửi tin nhắn riêng
    @PostMapping("/{chatId}/send")
    public ResponseEntity<?> sendPrivateMessage(@PathVariable Long chatId,
                                                @RequestBody Map<String, String> request,
                                                Authentication auth) {
        try {
            String content = request.get("content");
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Message content is required"));
            }

            User currentUser = userRepository.findByUsername(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            PrivateChat chat = privateChatRepository.findById(chatId)
                    .orElseThrow(() -> new RuntimeException("Chat not found"));

            // Kiểm tra quyền truy cập
            if (!chat.containsUser(currentUser)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied"));
            }

            // Tạo tin nhắn mới
            PrivateMessage message = new PrivateMessage();
            message.setPrivateChat(chat);
            message.setSender(currentUser);
            message.setContent(content.trim());
            message.setMessageType(PrivateMessage.MessageType.TEXT);
            message.setCreatedAt(LocalDateTime.now());
            message.setIsRead(false);

            message = privateMessageRepository.save(message);

            // Cập nhật lastMessageAt cho chat
            chat.setLastMessageAt(LocalDateTime.now());
            privateChatRepository.save(chat);

            return ResponseEntity.ok(Map.of(
                    "id", message.getId(),
                    "content", message.getContent(),
                    "sender", Map.of(
                            "username", currentUser.getUsername(),
                            "fullName", currentUser.getFullName() != null ? currentUser.getFullName() : currentUser.getUsername()
                    ),
                    "timestamp", message.getCreatedAt().toString(),
                    "messageType", message.getMessageType().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to send message: " + e.getMessage()
            ));
        }
    }
}