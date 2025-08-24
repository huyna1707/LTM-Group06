package uth.edu.appchat.Dtos;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GroupMessageDTO {
    private Long id;
    private Long groupId;
    private UserDTO sender;
    private String content;
    private LocalDateTime timestamp;

    public GroupMessageDTO(Long id, Long groupId, UserDTO sender, String content, LocalDateTime timestamp) {
        this.id = id;
        this.groupId = groupId;
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
    }
}

