package uth.edu.appchat.Models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name="file_attachments",
        indexes = {
                @Index(name="idx_fa_uploader", columnList="uploader_id"),
                @Index(name="idx_fa_pm", columnList="private_message_id"),
                @Index(name="idx_fa_gm", columnList="group_message_id"),
                @Index(name="idx_fa_uploaded", columnList="uploadedAt")
        })
// @Check(constraints="(private_message_id IS NOT NULL) + (group_message_id IS NOT NULL) = 1") // nếu MySQL hỗ trợ
@Data @NoArgsConstructor @AllArgsConstructor
public class FileAttachment {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=255) private String fileName;
    @Column(nullable=false, length=255) private String originalName;
    @Column(nullable=false, length=100) private String fileType;
    @Column(nullable=false) private Long fileSize;
    @Column(nullable=false, length=500) private String filePath;
    @Column(length=500) private String fileUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=20)
    private AttachmentType attachmentType;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="uploader_id", nullable=false,
            foreignKey = @ForeignKey(name="fk_fa_uploader"))
    private User uploader;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="private_message_id",
            foreignKey=@ForeignKey(name="fk_fa_private_msg"))
    private PrivateMessage privateMessage;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="group_message_id",
            foreignKey=@ForeignKey(name="fk_fa_group_msg"))
    private GroupMessage groupMessage;

    @CreationTimestamp
    @Column(nullable=false, updatable=false)
    private LocalDateTime uploadedAt;

    @Column(nullable=false)
    private Boolean isActive = true;

    // Enum cho loại file
    public enum AttachmentType {
        IMAGE,      // jpg, png, gif, webp
        VIDEO,      // mp4, avi, mov
        AUDIO,      // mp3, wav, ogg (voice message)
        DOCUMENT,   // pdf, doc, docx, txt
        ARCHIVE,    // zip, rar, 7z
        OTHER       // Các loại khác
    }

    // Helper methods
    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else if (fileSize < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", fileSize / (1024.0 * 1024.0 * 1024.0));
        }
    }

    public String getFileExtension() {
        if (originalName != null && originalName.contains(".")) {
            return originalName.substring(originalName.lastIndexOf("."));
        }
        return "";
    }

    public boolean isImage() {
        return attachmentType == AttachmentType.IMAGE;
    }

    public boolean isVideo() {
        return attachmentType == AttachmentType.VIDEO;
    }

    public boolean isAudio() {
        return attachmentType == AttachmentType.AUDIO;
    }

    public boolean isDocument() {
        return attachmentType == AttachmentType.DOCUMENT;
    }
}