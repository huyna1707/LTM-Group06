package uth.edu.appchat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")                // endpoint để client kết nối
                .setAllowedOriginPatterns("*")
                .withSockJS();                     // bật SockJS fallback (tùy chọn)
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");     // broker nội bộ, sẵn cho demo
        registry.setApplicationDestinationPrefixes("/app");
    }
}