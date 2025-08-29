package uth.edu.appchat.Dtos;

import lombok.Data;

import java.util.List;

@Data
public class MessageContentDTO {
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<AttachmentDTO> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<AttachmentDTO> attachments) {
        this.attachments = attachments;
    }

    private List<AttachmentDTO> attachments;
}