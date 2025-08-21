package uth.edu.appchat.Models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "group_messages")
@Data
public class GroupMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "group_chat_id", nullable = false)
    private GroupChat groupChat;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type")
    private MessageType messageType = MessageType.TEXT;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "is_pinned")
    private Boolean isPinned = false;

    @ManyToOne
    @JoinColumn(name = "pinned_by")
    private User pinnedBy;

    @Column(name = "pinned_at")
    private LocalDateTime pinnedAt;

    public enum MessageType {
        TEXT, IMAGE, FILE, SYSTEM
    }
}
