// src/main/java/uth/edu/appchat/Models/ChatClear.java
package uth.edu.appchat.Models;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "chat_clears")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatClear {

    @EmbeddedId
    private ChatClearId id;

    @Column(name = "cleared_at", nullable = false)
    private LocalDateTime clearedAt;

    @Embeddable
    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatClearId implements Serializable {
        private Long userId;    // ai bấm Xóa
        private String chatType; // "private"
        private Long chatId;     // id đoạn chat

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ChatClearId)) return false;
            ChatClearId that = (ChatClearId) o;
            return Objects.equals(userId, that.userId)
                    && Objects.equals(chatType, that.chatType)
                    && Objects.equals(chatId, that.chatId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, chatType, chatId);
        }
    }
}
