package uth.edu.appchat.Models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "private_chats",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_private_chat_user_pair", columnNames = {"user1_id", "user2_id"})
        },
        indexes = {
                @Index(name = "idx_private_chat_user1", columnList = "user1_id"),
                @Index(name = "idx_private_chat_user2", columnList = "user2_id"),
                @Index(name = "idx_private_chat_last_msg_at", columnList = "last_message_at")
        }
)
@Check(constraints = "user1_id <> user2_id")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user1", "user2", "messages"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PrivateChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user1_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_private_chat_user1"))
    private User user1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user2_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_private_chat_user2"))
    private User user2;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    // === Streak feature ===
    // number of consecutive days with at least one message
    // Some DB schemas still have a 'streak' column (INT NOT NULL). Map it to prevent insert errors.
    @Builder.Default
    @Column(name = "streak")
    private Integer streak = 0;

    @Column(name = "streak_count")
    private Integer streakCount;

    // date (server local) of the last day that contributed to streak (yyyy-MM-dd)
    @Column(name = "streak_last_date")
    private java.time.LocalDate streakLastDate;

    // number of recovery usages in the tracked month
    @Column(name = "streak_recovery_used")
    private Integer streakRecoveryUsed;

    @Column(name = "streak_recovery_month")
    private Integer streakRecoveryMonth; // 1-12

    @Column(name = "streak_recovery_year")
    private Integer streakRecoveryYear;

    // legacy DB column for recovery/restore count; present in DB as NOT NULL in some setups
    @Builder.Default
    @Column(name = "restore_count")
    private Integer restoreCount = 0;

    @OneToMany(mappedBy = "privateChat", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    @JsonIgnore
    @Builder.Default
    private List<PrivateMessage> messages = new ArrayList<>();

    /* === Biệt danh & version mới thêm === */
    @Column(name = "nickname", length = 100)
    private String nickname;

    @Column(name = "nickname_updated_by")
    private Long nicknameUpdatedBy;

    @Column(name = "nickname_updated_at")
    private LocalDateTime nicknameUpdatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /* === tiện ích === */
    public User getOtherUser(User currentUser) {
        if (currentUser == null) return null;
        if (currentUser.equals(user1)) return user2;
        if (currentUser.equals(user2)) return user1;
        return null;
    }

    public boolean containsUser(User user) {
        if (user == null) return false;
        return user.equals(user1) || user.equals(user2);
    }

    public void normalizePairById() {
        if (user1 != null && user2 != null
                && user1.getId() != null && user2.getId() != null
                && user1.getId() > user2.getId()) {
            User tmp = user1;
            user1 = user2;
            user2 = tmp;
        }
    }

    public void touchLastMessageAt() {
        this.lastMessageAt = LocalDateTime.now();
    }

    @PrePersist
    @PreUpdate
    private void enforcePairAndIntegrity() {
        if (user1 == null || user2 == null) {
            throw new IllegalStateException("PrivateChat requires both user1 and user2.");
        }
        if (user1.getId() != null && user2.getId() != null && user1.getId().equals(user2.getId())) {
            throw new IllegalStateException("user1 and user2 must be different.");
        }
        normalizePairById();
    }

    @Column(name = "cleared_at_user1")
    private LocalDateTime clearedAtUser1;

    @Column(name = "cleared_at_user2")
    private LocalDateTime clearedAtUser2;

    /** trả về mốc xóa cục bộ ứng với người dùng đang xem */
    public LocalDateTime getClearedAtFor(User u) {
        if (u == null) return null;
        if (u.equals(user1)) return clearedAtUser1;
        if (u.equals(user2)) return clearedAtUser2;
        return null;
    }

    /** đặt mốc xóa cục bộ cho người dùng đang xem */
    public void setClearedAtFor(User u, LocalDateTime t) {
        if (u == null) return;
        if (u.equals(user1)) this.clearedAtUser1 = t;
        else if (u.equals(user2)) this.clearedAtUser2 = t;
    }
}

