package uth.edu.appchat.Dtos;

import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMessage {
    public enum Type { JOIN, CHAT, LEAVE }
    private String sender;
    private String fullName;
    private String content;
    private String to;
    private Type type;
    private String timestamp;
}