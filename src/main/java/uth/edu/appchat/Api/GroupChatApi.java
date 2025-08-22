package uth.edu.appchat.Api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import uth.edu.appchat.Models.ChatStreak;
import uth.edu.appchat.Models.GroupChat;
import uth.edu.appchat.Models.User;
import uth.edu.appchat.Repositories.GroupChatRepository;
import uth.edu.appchat.Repositories.UserRepository;
import uth.edu.appchat.Services.ChatStreakService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/group-chat")
@RequiredArgsConstructor
public class GroupChatApi {

    private final GroupChatRepository groupChatRepository;
    private final UserRepository userRepository;
    private final ChatStreakService chatStreakService;

    // Lấy thông tin streak của group chat
    @GetMapping("/{groupId}/streak")
    public ResponseEntity<?> getGroupChatStreak(@PathVariable Long groupId, Authentication auth) {
        try {
            User currentUser = userRepository.findByUsername(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            GroupChat groupChat = groupChatRepository.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("Group chat not found"));

            Optional<ChatStreak> streakOpt = chatStreakService.getGroupChatStreak(groupChat);
            
            if (streakOpt.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "currentStreak", 0,
                        "bestStreak", 0,
                        "display", "",
                        "canRestore", false,
                        "weeklyRestoresUsed", 0
                ));
            }

            ChatStreak streak = streakOpt.get();
            return ResponseEntity.ok(Map.of(
                    "currentStreak", streak.getCurrentStreak(),
                    "bestStreak", streak.getBestStreak(),
                    "display", streak.getStreakDisplay(),
                    "canRestore", streak.canRestore(),
                    "weeklyRestoresUsed", streak.getWeeklyRestoresUsed(),
                    "streakStartDate", streak.getStreakStartDate() != null ? streak.getStreakStartDate().toString() : null,
                    "lastMessageDate", streak.getLastMessageDate() != null ? streak.getLastMessageDate().toString() : null
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to get group streak info: " + e.getMessage()
            ));
        }
    }

    // Restore streak cho group chat
    @PostMapping("/{groupId}/restore-streak")
    public ResponseEntity<?> restoreGroupStreak(@PathVariable Long groupId, Authentication auth) {
        try {
            User currentUser = userRepository.findByUsername(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            GroupChat groupChat = groupChatRepository.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("Group chat not found"));

            Optional<ChatStreak> streakOpt = chatStreakService.getGroupChatStreak(groupChat);
            if (streakOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No streak found"));
            }

            boolean restored = chatStreakService.restoreStreak(streakOpt.get().getId());
            
            if (restored) {
                return ResponseEntity.ok(Map.of(
                        "message", "Group streak restored successfully",
                        "streak", chatStreakService.getGroupChatStreakDisplay(groupChat)
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Cannot restore streak. Either no restores left or conditions not met."
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to restore group streak: " + e.getMessage()
            ));
        }
    }

    // Lấy top streaks của private chats
    @GetMapping("/streaks/top-private")
    public ResponseEntity<?> getTopPrivateStreaks(Authentication auth) {
        try {
            User currentUser = userRepository.findByUsername(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<ChatStreak> topStreaks = chatStreakService.getTopPrivateStreaks();
            
            List<Map<String, Object>> streakList = topStreaks.stream()
                    .limit(10) // Top 10
                    .map(streak -> {
                        if (streak.getPrivateChat() != null) {
                            User otherUser = streak.getPrivateChat().getOtherUser(currentUser);
                            if (otherUser == null) {
                                // Nếu current user không thuộc chat này, lấy user1
                                otherUser = streak.getPrivateChat().getUser1();
                            }
                            
                            return Map.<String, Object>of(
                                    "chatId", streak.getPrivateChat().getId(),
                                    "chatType", "private",
                                    "chatName", otherUser.getFullName() != null ? otherUser.getFullName() : otherUser.getUsername(),
                                    "currentStreak", streak.getCurrentStreak(),
                                    "bestStreak", streak.getBestStreak(),
                                    "display", streak.getStreakDisplay()
                            );
                        }
                        return null;
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(streakList);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to get top private streaks: " + e.getMessage()
            ));
        }
    }

    // Lấy top streaks của group chats
    @GetMapping("/streaks/top-groups")
    public ResponseEntity<?> getTopGroupStreaks(Authentication auth) {
        try {
            List<ChatStreak> topStreaks = chatStreakService.getTopGroupStreaks();
            
            List<Map<String, Object>> streakList = topStreaks.stream()
                    .limit(10) // Top 10
                    .map(streak -> {
                        if (streak.getGroupChat() != null) {
                            return Map.<String, Object>of(
                                    "chatId", streak.getGroupChat().getId(),
                                    "chatType", "group",
                                    "chatName", streak.getGroupChat().getName(),
                                    "currentStreak", streak.getCurrentStreak(),
                                    "bestStreak", streak.getBestStreak(),
                                    "display", streak.getStreakDisplay()
                            );
                        }
                        return null;
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(streakList);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to get top group streaks: " + e.getMessage()
            ));
        }
    }
}
