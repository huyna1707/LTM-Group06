package uth.edu.appchat.Api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import uth.edu.appchat.Dtos.AttachmentDTO;
import uth.edu.appchat.Dtos.MessageContentDTO;
import uth.edu.appchat.Models.ChatClear;
import uth.edu.appchat.Models.PrivateChat;
import uth.edu.appchat.Models.PrivateMessage;
import uth.edu.appchat.Models.User;
import uth.edu.appchat.Repositories.ChatClearRepository;
import uth.edu.appchat.Repositories.PrivateChatRepository;
import uth.edu.appchat.Repositories.PrivateMessageRepository;
import uth.edu.appchat.Repositories.UserRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/private-chat")
@RequiredArgsConstructor
public class PrivateChatApi {

    private final PrivateChatRepository privateChatRepository;
    private final PrivateMessageRepository privateMessageRepository;
    private final UserRepository userRepository;
    private final ChatClearRepository chatClearRepository;
    private final SimpMessagingTemplate messaging;

    // ===========================
    // Lấy danh sách chat riêng của user hiện tại
    // ===========================
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
                                "fullName", Optional.ofNullable(otherUser.getFullName()).orElse(otherUser.getUsername()),
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

    // ===========================
    // Tạo hoặc lấy chat riêng với user khác
    // ===========================
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
                            "fullName", Optional.ofNullable(targetUser.getFullName()).orElse(targetUser.getUsername()),
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

    // ===========================
    // Lấy tin nhắn của chat riêng (lọc theo clearedAt của user hiện tại)
    // ===========================
    @GetMapping("/{chatId}/messages")
    public ResponseEntity<?> getPrivateChatMessages(@PathVariable Long chatId, Authentication auth) {
        try {
            User currentUser = userRepository.findByUsername(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            PrivateChat chat = privateChatRepository.findById(chatId)
                    .orElseThrow(() -> new RuntimeException("Chat not found"));

            if (!chat.containsUser(currentUser)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied"));
            }

            // lấy clearedAt theo user hiện tại
            ChatClear.ChatClearId id = new ChatClear.ChatClearId(currentUser.getId(), "private", chatId);
            LocalDateTime clearedAt = chatClearRepository.findById(id)
                    .map(ChatClear::getClearedAt)
                    .orElse(LocalDateTime.MIN);

            // nếu clearedAt > now (đồng hồ lệch) => bỏ lọc
            if (clearedAt.isAfter(LocalDateTime.now())) {
                clearedAt = LocalDateTime.MIN;
            }

            List<PrivateMessage> messages =
                    privateMessageRepository.findByPrivateChatAndCreatedAtAfterOrderByCreatedAtAsc(chat, clearedAt);

            // map DTO trả về
            List<Map<String, Object>> dto = messages.stream().map(msg -> {
                User sender = msg.getSender();
                Map<String, Object> base = new LinkedHashMap<>();
                base.put("id", msg.getId());
                base.put("sender", Map.of(
                        "username", sender.getUsername(),
                        "fullName", Optional.ofNullable(sender.getFullName()).orElse(sender.getUsername())
                ));
                base.put("timestamp", msg.getCreatedAt().toString());
                base.put("messageType", msg.getMessageType().toString());
                base.put("isRead", Optional.ofNullable(msg.getIsRead()).orElse(false));

                if (msg.getMessageType() == PrivateMessage.MessageType.TEXT) {
                    base.put("content", Optional.ofNullable(msg.getContent()).orElse(""));
                    base.put("attachments", List.of());
                } else {
                    String url = Optional.ofNullable(msg.getContent()).orElse("");
                    String type = switch (msg.getMessageType()) {
                        case IMAGE -> "image";
                        case VIDEO -> "video";
                        default    -> "file";
                    };
                    Map<String, Object> att = new LinkedHashMap<>();
                    att.put("type", type);
                    att.put("url", url);
                    att.put("name", url.contains("/") ? url.substring(url.lastIndexOf('/') + 1) : url);
                    att.put("size", null);

                    base.put("content", "");
                    base.put("attachments", List.of(att));
                }
                return base;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to get messages: " + e.getMessage()
            ));
        }
    }

    // ===========================
    // Gửi tin nhắn riêng (realtime cho TEXT & ATTACHMENTS)
    // ===========================
    @PostMapping("/{chatId}/send")
    public ResponseEntity<?> sendPrivateMessage(@PathVariable Long chatId,
                                                @RequestBody MessageContentDTO req,
                                                Authentication auth) {
        try {
            String content = Optional.ofNullable(req).map(MessageContentDTO::getContent).orElse("").trim();
            List<AttachmentDTO> atts = Optional.ofNullable(req).map(MessageContentDTO::getAttachments).orElse(List.of());
            if (content.isBlank() && atts.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Message content or attachments is required"));
            }

            User me = userRepository.findByUsername(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            PrivateChat chat = privateChatRepository.findById(chatId)
                    .orElseThrow(() -> new RuntimeException("Chat not found"));

            if (!chat.containsUser(me)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied"));
            }

            User other = chat.getOtherUser(me);

            // TEXT
            if (!content.isBlank()) {
                PrivateMessage m = new PrivateMessage();
                m.setPrivateChat(chat);
                m.setSender(me);
                m.setContent(content);
                m.setMessageType(PrivateMessage.MessageType.TEXT);
                m.setCreatedAt(LocalDateTime.now());
                m.setIsRead(false);
                m = privateMessageRepository.save(m);

                Map<String, Object> dto = new LinkedHashMap<>();
                dto.put("id", m.getId());
                dto.put("chatId", chatId);
                dto.put("type", "CHAT");
                dto.put("messageType", "TEXT");
                dto.put("sender", Map.of(
                        "username", me.getUsername(),
                        "fullName", Optional.ofNullable(me.getFullName()).orElse(me.getUsername())
                ));
                dto.put("content", m.getContent());
                dto.put("attachments", List.of());
                dto.put("timestamp", m.getCreatedAt().toString());

                messaging.convertAndSendToUser(other.getUsername(), "/queue/private", dto);
                messaging.convertAndSendToUser(me.getUsername(),    "/queue/private", dto);
            }

            // ATTACHMENTS
            for (AttachmentDTO a : atts) {
                String url = Optional.ofNullable(a.getUrl()).orElse("").trim();
                if (url.isEmpty()) continue;

                PrivateMessage.MessageType mt = switch (Optional.ofNullable(a.getType()).orElse("").toLowerCase()) {
                    case "image" -> PrivateMessage.MessageType.IMAGE;
                    case "video" -> PrivateMessage.MessageType.VIDEO;
                    default      -> PrivateMessage.MessageType.FILE;
                };

                PrivateMessage m = new PrivateMessage();
                m.setPrivateChat(chat);
                m.setSender(me);
                m.setContent(url);
                m.setMessageType(mt);
                m.setCreatedAt(LocalDateTime.now());
                m.setIsRead(false);
                m = privateMessageRepository.save(m);

                String attType = switch (mt) {
                    case IMAGE -> "image";
                    case VIDEO -> "video";
                    default    -> "file";
                };
                String name = url.contains("/") ? url.substring(url.lastIndexOf('/') + 1) : url;

                Map<String, Object> att = new LinkedHashMap<>();
                att.put("type", attType);
                att.put("url", url);
                att.put("name", name);
                att.put("size", null);

                Map<String, Object> dto = new LinkedHashMap<>();
                dto.put("id", m.getId());
                dto.put("chatId", chatId);
                dto.put("type", "CHAT");
                dto.put("messageType", attType.toUpperCase());
                dto.put("sender", Map.of(
                        "username", me.getUsername(),
                        "fullName", Optional.ofNullable(me.getFullName()).orElse(me.getUsername())
                ));
                dto.put("content", "");
                dto.put("attachments", List.of(att));
                dto.put("timestamp", m.getCreatedAt().toString());

                messaging.convertAndSendToUser(other.getUsername(), "/queue/private", dto);
                messaging.convertAndSendToUser(me.getUsername(),    "/queue/private", dto);
            }

            chat.setLastMessageAt(LocalDateTime.now());
            privateChatRepository.save(chat);

            return ResponseEntity.ok(Map.of("ok", true));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to send message: " + e.getMessage()));
        }
    }

    // ===========================
    // Xoá đoạn chat ở phía user hiện tại (không ảnh hưởng người kia)
    // ===========================
    @PostMapping("/{chatId}/clear")
    public ResponseEntity<?> clearPrivateChat(@PathVariable Long chatId, Authentication auth) {
        try {
            User currentUser = userRepository.findByUsername(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            PrivateChat chat = privateChatRepository.findById(chatId)
                    .orElseThrow(() -> new RuntimeException("Chat not found"));

            if (!chat.containsUser(currentUser)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error","Access denied"));
            }

            ChatClear.ChatClearId id = new ChatClear.ChatClearId(currentUser.getId(), "private", chatId);
            ChatClear rec = chatClearRepository.findById(id)
                    .orElseGet(() -> new ChatClear(id, LocalDateTime.now()));

            // tạo “khoảng hở” 1ns để tin mới ngay-sau-khi-clear không bị lọc nhầm
            rec.setClearedAt(LocalDateTime.now().minusNanos(1));
            chatClearRepository.save(rec);

            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error","Failed to clear chat: "+e.getMessage()));
        }
    }
}
