
package uth.edu.appchat.Models;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name="group_messages",
        indexes = {
                @Index(name="idx_gm_group", columnList="group_chat_id"),
                @Index(name="idx_gm_sender", columnList="sender_id"),
                @Index(name="idx_gm_created", columnList="created_at")
        })
@Data
public class GroupMessage {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="group_chat_id", nullable=false,
            foreignKey=@ForeignKey(name="fk_gmsg_group"))
    private GroupChat groupChat;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="sender_id", nullable=false,
            foreignKey=@ForeignKey(name="fk_gmsg_sender"))
    private User sender;

    @Column(columnDefinition="TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name="message_type", nullable=false, length=10)
    private MessageType messageType = MessageType.TEXT;

    @CreationTimestamp
    @Column(name="created_at", nullable=false, updatable=false)
    private LocalDateTime createdAt;

    @Column(name="delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name="is_pinned", nullable=false)
    private Boolean isPinned = false;

    public enum MessageType { TEXT, IMAGE, SYSTEM, FILE }
}
