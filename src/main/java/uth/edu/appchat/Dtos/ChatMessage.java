package uth.edu.appchat.Dtos;

import lombok.Data;

@Data
public class ChatMessage {
    public enum Type { CHAT, JOIN, LEAVE }
    private Type type;
    private String content;
    private String sender;
    private String to;
    private long timestamp;
}