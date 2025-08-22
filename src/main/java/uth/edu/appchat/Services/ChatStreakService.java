package uth.edu.appchat.Services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uth.edu.appchat.Models.ChatStreak;
import uth.edu.appchat.Models.GroupChat;
import uth.edu.appchat.Models.PrivateChat;
import uth.edu.appchat.Repositories.ChatStreakRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatStreakService {

    private final ChatStreakRepository chatStreakRepository;

    /**
     * Cập nhật streak khi có tin nhắn mới trong private chat
     */
    @Transactional
    public ChatStreak updatePrivateChatStreak(PrivateChat privateChat) {
        Optional<ChatStreak> existingStreak = chatStreakRepository.findByPrivateChat(privateChat);
        
        ChatStreak streak = existingStreak.orElse(ChatStreak.builder()
                .privateChat(privateChat)
                .build());

        streak.updateStreak(LocalDate.now());
        return chatStreakRepository.save(streak);
    }

    /**
     * Cập nhật streak khi có tin nhắn mới trong group chat
     */
    @Transactional
    public ChatStreak updateGroupChatStreak(GroupChat groupChat) {
        Optional<ChatStreak> existingStreak = chatStreakRepository.findByGroupChat(groupChat);
        
        ChatStreak streak = existingStreak.orElse(ChatStreak.builder()
                .groupChat(groupChat)
                .build());

        streak.updateStreak(LocalDate.now());
        return chatStreakRepository.save(streak);
    }

    /**
     * Lấy streak hiển thị cho private chat
     */
    public String getPrivateChatStreakDisplay(PrivateChat privateChat) {
        Optional<ChatStreak> streak = chatStreakRepository.findByPrivateChat(privateChat);
        return streak.map(ChatStreak::getStreakDisplay).orElse("");
    }

    /**
     * Lấy streak hiển thị cho group chat
     */
    public String getGroupChatStreakDisplay(GroupChat groupChat) {
        Optional<ChatStreak> streak = chatStreakRepository.findByGroupChat(groupChat);
        return streak.map(ChatStreak::getStreakDisplay).orElse("");
    }

    /**
     * Lấy thông tin chi tiết streak cho private chat
     */
    public Optional<ChatStreak> getPrivateChatStreak(PrivateChat privateChat) {
        return chatStreakRepository.findByPrivateChat(privateChat);
    }

    /**
     * Lấy thông tin chi tiết streak cho group chat
     */
    public Optional<ChatStreak> getGroupChatStreak(GroupChat groupChat) {
        return chatStreakRepository.findByGroupChat(groupChat);
    }

    /**
     * Lấy streak theo private chat ID
     */
    public Optional<ChatStreak> getPrivateChatStreakById(Long privateChatId) {
        return chatStreakRepository.findByPrivateChatId(privateChatId);
    }

    /**
     * Lấy streak theo group chat ID
     */
    public Optional<ChatStreak> getGroupChatStreakById(Long groupChatId) {
        return chatStreakRepository.findByGroupChatId(groupChatId);
    }

    /**
     * Restore streak (sử dụng lượt restore cho các ngày bỏ lỡ)
     */
    @Transactional
    public boolean restoreStreak(Long streakId) {
        Optional<ChatStreak> streakOpt = chatStreakRepository.findById(streakId);
        if (streakOpt.isEmpty()) {
            return false;
        }

        ChatStreak streak = streakOpt.get();
        LocalDate today = LocalDate.now();
        
        if (streak.getLastMessageDate() == null) {
            return false;
        }
        
        // Tính số ngày bỏ lỡ
        long daysSinceLastMessage = streak.getLastMessageDate().until(today).getDays();
        
        if (daysSinceLastMessage <= 0) {
            return false; // Không có ngày nào bỏ lỡ
        }
        
        // Kiểm tra có đủ lượt restore không
        int restoresNeeded = (int) daysSinceLastMessage;
        if (streak.getWeeklyRestoresUsed() + restoresNeeded > 2) {
            return false; // Không đủ lượt restore
        }
        
        // Thực hiện restore
        streak.setWeeklyRestoresUsed(streak.getWeeklyRestoresUsed() + restoresNeeded);
        streak.setCurrentStreak(streak.getCurrentStreak() + 1);
        streak.setLastMessageDate(today);
        streak.setUpdatedAt(LocalDateTime.now());
        
        // Cập nhật best streak nếu cần
        if (streak.getCurrentStreak() > streak.getBestStreak()) {
            streak.setBestStreak(streak.getCurrentStreak());
        }
        
        chatStreakRepository.save(streak);
        return true;
    }

    /**
     * Scheduled task chạy hàng ngày để kiểm tra và reset các streak đã hết hạn
     * Chạy lúc 1:00 AM mỗi ngày
     */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void checkAndResetExpiredStreaks() {
        log.info("Checking for expired streaks...");
        
        LocalDate today = LocalDate.now();
        LocalDate cutoffDate = today.minusDays(1); // Kiểm tra từ hôm qua trở về trước
        
        List<ChatStreak> streaksToCheck = chatStreakRepository.findStreaksToCheck(cutoffDate);
        
        int resetCount = 0;
        for (ChatStreak streak : streaksToCheck) {
            if (streak.shouldResetStreak(today)) {
                log.info("Resetting expired streak: ID={}, currentStreak={}, lastMessageDate={}, restoresUsed={}", 
                        streak.getId(), streak.getCurrentStreak(), streak.getLastMessageDate(), streak.getWeeklyRestoresUsed());
                streak.resetStreak();
                chatStreakRepository.save(streak);
                resetCount++;
            }
        }
        
        log.info("Reset {} expired streaks", resetCount);
    }

    /**
     * Lấy top streak của private chats
     */
    public List<ChatStreak> getTopPrivateStreaks() {
        return chatStreakRepository.findTopPrivateStreaks();
    }

    /**
     * Lấy top streak của group chats
     */
    public List<ChatStreak> getTopGroupStreaks() {
        return chatStreakRepository.findTopGroupStreaks();
    }

    /**
     * Kiểm tra thủ công và reset streak nếu cần
     */
    @Transactional
    public void checkAndResetStreakIfNeeded(ChatStreak streak) {
        if (streak.shouldResetStreak(LocalDate.now())) {
            streak.resetStreak();
            chatStreakRepository.save(streak);
        }
    }
}
