package uth.edu.appchat.Dtos;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class GroupMessageDTO {
    private Long id;
    private Long groupId;
    private UserDTO sender;
    private String content;
    private LocalDateTime timestamp;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public UserDTO getSender() {
        return sender;
    }

    public void setSender(UserDTO sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public List<AttachmentDTO> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<AttachmentDTO> attachments) {
        this.attachments = attachments;
    }

    private String messageType;
    private List<AttachmentDTO> attachments;
    public GroupMessageDTO(Long id, Long groupId, UserDTO sender, String content, LocalDateTime timestamp) {
        this.id = id;
        this.groupId = groupId;
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
    }
}

