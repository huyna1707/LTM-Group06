package uth.edu.appchat.Models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_attachments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileAttachment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 255)
    private String fileName;        // Tên file được lưu trên server: "abc123.jpg"
    
    @Column(nullable = false, length = 255)
    private String originalName;    // Tên file gốc từ user: "Ảnh đẹp.jpg"
    
    @Column(nullable = false, length = 100)
    private String fileType;        // MIME type: "image/jpeg", "application/pdf"
    
    @Column(nullable = false)
    private Long fileSize;          // Kích thước file (bytes)
    
    @Column(nullable = false, length = 500)
    private String filePath;        // Đường dẫn file trên server: "/uploads/2025/08/abc123.jpg"
    
    @Column(length = 500)
    private String fileUrl;         // URL public để truy cập file
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttachmentType attachmentType;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;          // Người upload file
    
    // Liên kết với tin nhắn private
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "private_message_id")
    private PrivateMessage privateMessage;
    
    // Liên kết với tin nhắn group
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_message_id")
    private GroupMessage groupMessage;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;
    
    @Column(nullable = false)
    private Boolean isActive = true; // Soft delete
    
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
