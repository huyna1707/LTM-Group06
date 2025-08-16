package com.example.demo.Controller;

import com.example.demo.model.ChatMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    // Client gửi tới /app/chat.send
    @MessageMapping("/chat.send")
    @SendTo("/topic/public")
    public ChatMessage send(@Payload ChatMessage msg) {
        if (msg.getTimestamp() == 0) msg.setTimestamp(System.currentTimeMillis());
        return msg; // được broker phát tới mọi client đang subscribe /topic/public
    }
}
