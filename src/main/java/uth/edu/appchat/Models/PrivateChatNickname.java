// PrivateChatNickname.java
package uth.edu.appchat.Models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "private_chat_nicknames",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_privnick_chat_owner_target",
                columnNames = {"chat_id","owner_id","target_id"}
        ),
        indexes = {
                @Index(name="idx_privnick_chat_owner", columnList="chat_id,owner_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PrivateChatNickname {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="chat_id", nullable=false)
    private PrivateChat chat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="owner_id", nullable=false)     // ai đặt biệt danh
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="target_id", nullable=false)    // đặt cho ai (đối phương)
    private User target;

    @Column(name="nickname", length=100)
    private String nickname;                         // null/"" = bỏ biệt danh

    @Column(name="updated_at", nullable=false)
    private LocalDateTime updatedAt;
}
