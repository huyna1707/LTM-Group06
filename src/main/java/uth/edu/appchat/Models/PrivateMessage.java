package uth.edu.appchat.Models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "private_messages",
        indexes = {
                @Index(name = "idx_pm_chat", columnList = "private_chat_id"),
                @Index(name = "idx_pm_sender", columnList = "sender_id"),
                @Index(name = "idx_pm_created_at", columnList = "created_at"),
                // đổi tên cột tránh keyword MySQL "read"
                @Index(name = "idx_pm_is_read", columnList = "is_read")
        }
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"privateChat", "sender", "attachments"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PrivateMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // --- Relations ---
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "private_chat_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_pm_chat")
    )
    @JsonIgnore
    private PrivateChat privateChat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "sender_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_pm_sender")
    )
    private User sender;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "delivered", nullable = false)
    @Builder.Default
    private Boolean delivered = false;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    // đổi tên cột read -> is_read
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    // ====== DELETE ONLY ======
    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "delete_type", length = 20)
    private DeleteType deleteType;

    // ====== FILE ATTACHMENTS ======
    @OneToMany(
            mappedBy = "privateMessage",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("uploadedAt ASC")
    @Builder.Default
    private List<FileAttachment> attachments = new ArrayList<>();

    // ====== Enums ======
    public enum MessageType { TEXT, IMAGE, FILE, VIDEO, STICKER }
    public enum DeleteType { FOR_ME, FOR_EVERYONE }

    // ====== Lifecycle ======
    @PrePersist
    private void prePersistDefaults() {
        if (messageType == null) messageType = MessageType.TEXT;
        if (delivered == null) delivered = false;
        if (isRead == null) isRead = false;
        if (deleted == null) deleted = false;
    }

    // ====== STATUS HELPERS ======
    public void markAsDelivered() {
        if (Boolean.TRUE.equals(this.delivered)) return;
        this.delivered = true;
        this.deliveredAt = LocalDateTime.now();
    }

    public void markAsRead() {
        if (Boolean.TRUE.equals(this.isRead)) return;
        this.isRead = true;
        this.readAt = LocalDateTime.now();
        if (!Boolean.TRUE.equals(this.delivered)) {
            markAsDelivered();
        }
    }

    public String getMessageStatus() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
        if (readAt != null) return "Đã xem " + readAt.format(fmt);
        if (deliveredAt != null) return "Đã gửi " + deliveredAt.format(fmt);
        return "Đang gửi...";
    }

    /** ✓ (đã gửi), ✓✓ xám (đã đến), ✓✓ xanh (đã xem) – tuỳ UI bạn map màu */
    public String getStatusIcon() {
        if (Boolean.TRUE.equals(isRead)) return "viewed";      // viewed
        if (Boolean.TRUE.equals(delivered)) return "delivered"; // delivered
        return "sending";                                      // sending
    }

    // ====== DELETE HELPERS ======
    public void deleteMessage(DeleteType type) {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deleteType = type;
        if (type == DeleteType.FOR_EVERYONE) {
            this.content = "🚫 Tin nhắn này đã được xóa";
        }
    }

    public String getDeletedText() {
        return (deleteType == DeleteType.FOR_EVERYONE)
                ? "🚫 Tin nhắn này đã được xóa"
                : "🚫 Bạn đã xóa tin nhắn này";
    }

    // ====== ATTACHMENT HELPERS ======
    public boolean hasAttachments() { return attachments != null && !attachments.isEmpty(); }

    public int getAttachmentCount() { return hasAttachments() ? attachments.size() : 0; }

    public List<FileAttachment> getImageAttachments() {
        if (!hasAttachments()) return List.of();
        return attachments.stream().filter(FileAttachment::isImage).toList();
    }

    public List<FileAttachment> getDocumentAttachments() {
        if (!hasAttachments()) return List.of();
        return attachments.stream().filter(FileAttachment::isDocument).toList();
    }

    // ====== DISPLAY HELPERS ======
    public String getDisplayContent() {
        if (Boolean.TRUE.equals(deleted)) return getDeletedText();

        if (content == null || content.trim().isEmpty()) {
            if (hasAttachments()) {
                return getAttachmentCount() == 1
                        ? "📎 1 tệp đính kèm"
                        : "📎 " + getAttachmentCount() + " tệp đính kèm";
            }
            return "";
        }
        return content;
    }

    public String getTimeDisplay() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
        return createdAt != null ? createdAt.format(fmt) : "";
    }
}
