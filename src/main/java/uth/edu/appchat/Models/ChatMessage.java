package uth.edu.appchat.Models;

public class ChatMessage {
    private String from;
    private String content;
    private long timestamp;

    public ChatMessage() {}
    public ChatMessage(String from, String content, long timestamp) {
        this.from = from; this.content = content; this.timestamp = timestamp;
    }
    public String getFrom(){ return from; }
    public void setFrom(String from){ this.from = from; }
    public String getContent(){ return content; }
    public void setContent(String content){ this.content = content; }
    public long getTimestamp(){ return timestamp; }
    public void setTimestamp(long ts){ this.timestamp = ts; }
}
