package uth.edu.appchat.Controllers;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import uth.edu.appchat.Dtos.ChatMessage;

@Controller
public class ChatController {

    private final SimpMessagingTemplate template;
    public ChatController(SimpMessagingTemplate template) { this.template = template; }

    // Broadcast phòng chung
    @MessageMapping("/chat.send")
    @SendTo("/topic/public")
    public ChatMessage send(ChatMessage msg) {
        msg.setTimestamp(System.currentTimeMillis());
        msg.setType(ChatMessage.Type.CHAT);
        return msg;
    }

    @MessageMapping("/chat.private")
    public void sendPrivate(ChatMessage msg) {
        msg.setTimestamp(System.currentTimeMillis());
        template.convertAndSendToUser(msg.getTo(), "/queue/messages", msg);
    }
}