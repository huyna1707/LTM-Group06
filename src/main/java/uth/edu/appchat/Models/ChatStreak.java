package uth.edu.appchat.Models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_streaks",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_chat_streak_private", columnNames = {"private_chat_id"}),
                @UniqueConstraint(name = "uk_chat_streak_group", columnNames = {"group_chat_id"})
        },
        indexes = {
                @Index(name = "idx_chat_streak_private", columnList = "private_chat_id"),
                @Index(name = "idx_chat_streak_group", columnList = "group_chat_id"),
                @Index(name = "idx_chat_streak_last_msg", columnList = "last_message_date")
        }
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ChatStreak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // Streak cho private chat
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "private_chat_id",
            foreignKey = @ForeignKey(name = "fk_chat_streak_private"))
    private PrivateChat privateChat;

    // Streak cho group chat
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_chat_id",
            foreignKey = @ForeignKey(name = "fk_chat_streak_group"))
    private GroupChat groupChat;

    // Số ngày streak hiện tại
    @Column(name = "current_streak", nullable = false)
    @Builder.Default
    private Integer currentStreak = 0;

    // Ngày bắt đầu streak hiện tại
    @Column(name = "streak_start_date")
    private LocalDate streakStartDate;

    // Ngày gửi tin nhắn cuối cùng để tính streak
    @Column(name = "last_message_date")
    private LocalDate lastMessageDate;

    // Số lần được restore trong tuần hiện tại (tối đa 2)
    @Column(name = "weekly_restores_used", nullable = false)
    @Builder.Default
    private Integer weeklyRestoresUsed = 0;

    // Ngày đầu tuần hiện tại để reset restore count
    @Column(name = "current_week_start")
    private LocalDate currentWeekStart;

    // Streak cao nhất từng đạt được
    @Column(name = "best_streak", nullable = false)
    @Builder.Default
    private Integer bestStreak = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Kiểm tra có thể restore streak không
     */
    public boolean canRestore() {
        return weeklyRestoresUsed < 2;
    }

    /**
     * Sử dụng 1 lần restore
     */
    public void useRestore() {
        if (canRestore()) {
            weeklyRestoresUsed++;
        }
    }

    /**
     * Reset số lần restore nếu sang tuần mới
     */
    public void resetWeeklyRestoresIfNeeded(LocalDate today) {
        LocalDate mondayOfThisWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
        if (currentWeekStart == null || !currentWeekStart.equals(mondayOfThisWeek)) {
            currentWeekStart = mondayOfThisWeek;
            weeklyRestoresUsed = 0;
        }
    }

    /**
     * Cập nhật streak với tin nhắn mới
     */
    public void updateStreak(LocalDate messageDate) {
        resetWeeklyRestoresIfNeeded(messageDate);
        
        if (lastMessageDate == null) {
            // Tin nhắn đầu tiên
            currentStreak = 1;
            streakStartDate = messageDate;
            lastMessageDate = messageDate;
        } else if (messageDate.equals(lastMessageDate)) {
            // Cùng ngày, không thay đổi streak
            return;
        } else if (messageDate.equals(lastMessageDate.plusDays(1))) {
            // Ngày tiếp theo, tăng streak
            currentStreak++;
            lastMessageDate = messageDate;
        } else if (messageDate.isAfter(lastMessageDate.plusDays(1))) {
            // Bỏ lỡ ít nhất 1 ngày
            long daysMissed = lastMessageDate.until(messageDate).getDays() - 1; // Số ngày bỏ lỡ
            
            if (daysMissed <= weeklyRestoresUsed + (2 - weeklyRestoresUsed)) {
                // Có thể restore (mỗi ngày bỏ lỡ cần 1 lượt restore)
                int restoresNeeded = (int) daysMissed;
                if (weeklyRestoresUsed + restoresNeeded <= 2) {
                    // Đủ lượt restore
                    weeklyRestoresUsed += restoresNeeded;
                    currentStreak++;
                    lastMessageDate = messageDate;
                } else {
                    // Không đủ lượt restore - reset streak
                    currentStreak = 1;
                    streakStartDate = messageDate;
                    lastMessageDate = messageDate;
                }
            } else {
                // Quá nhiều ngày bỏ lỡ - reset streak
                currentStreak = 1;
                streakStartDate = messageDate;
                lastMessageDate = messageDate;
            }
        }

        // Cập nhật best streak
        if (currentStreak > bestStreak) {
            bestStreak = currentStreak;
        }

        updatedAt = LocalDateTime.now();
    }

    /**
     * Lấy emoji cho streak
     */
    public String getStreakDisplay() {
        if (currentStreak >= 3) {
            return currentStreak + "🔥";
        }
        return "";
    }

    /**
     * Kiểm tra streak có cần reset không (hết lượt restore)
     */
    public boolean shouldResetStreak(LocalDate today) {
        if (lastMessageDate == null) return false;
        
        long daysSinceLastMessage = lastMessageDate.until(today).getDays();
        
        // Nếu không nhắn tin hôm nay
        if (daysSinceLastMessage > 0) {
            // Tính số ngày bỏ lỡ (không tính ngày hôm nay)
            long daysMissed = daysSinceLastMessage;
            
            // Kiểm tra xem còn đủ lượt restore không
            int restoresNeeded = (int) daysMissed;
            return (weeklyRestoresUsed + restoresNeeded) > 2;
        }
        
        return false;
    }

    /**
     * Reset streak về 0
     */
    public void resetStreak() {
        currentStreak = 0;
        streakStartDate = null;
        lastMessageDate = null;
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
