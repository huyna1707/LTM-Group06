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
                // đảm bảo 1 cặp (user1, user2) chỉ có 1 cuộc chat
                @UniqueConstraint(name = "uk_private_chat_user_pair", columnNames = {"user1_id", "user2_id"})
        },
        indexes = {
                @Index(name = "idx_private_chat_user1", columnList = "user1_id"),
                @Index(name = "idx_private_chat_user2", columnList = "user2_id"),
                @Index(name = "idx_private_chat_last_msg_at", columnList = "last_message_at")
        }
)
// chặn trường hợp user1_id = user2_id
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

    // luôn để LAZY để tránh n+1 và payload lớn
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

    // cập nhật ở service khi có tin nhắn mới
    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @OneToMany(
            mappedBy = "privateChat",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("createdAt ASC")
    @JsonIgnore // tránh vòng lặp khi serialize Chat -> messages -> chat...
    @Builder.Default
    private List<PrivateMessage> messages = new ArrayList<>();

    /** trả về người còn lại trong phòng (null nếu không thuộc phòng) */
    public User getOtherUser(User currentUser) {
        if (currentUser == null) return null;
        if (currentUser.equals(user1)) return user2;
        if (currentUser.equals(user2)) return user1;
        return null;
    }

    /** kiểm tra user có thuộc phòng này không */
    public boolean containsUser(User user) {
        if (user == null) return false;
        return user.equals(user1) || user.equals(user2);
    }

    /** chuẩn hoá cặp user theo ID (user1.id < user2.id) để không trùng (A-B vs B-A) */
    public void normalizePairById() {
        if (user1 != null && user2 != null
                && user1.getId() != null && user2.getId() != null
                && user1.getId() > user2.getId()) {
            User tmp = user1;
            user1 = user2;
            user2 = tmp;
        }
    }

    /** cập nhật mốc thời gian tin nhắn cuối cùng */
    public void touchLastMessageAt() {
        this.lastMessageAt = LocalDateTime.now();
    }

    // đảm bảo chuẩn hoá trước khi lưu/cập nhật
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
}
