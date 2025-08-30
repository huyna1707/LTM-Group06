package uth.edu.appchat.Dtos;
import java.time.LocalDateTime;
// NicknameResponse.java
public record NicknameResponse(
        Long chatId, String scope, String nickname,
        Long updatedBy, LocalDateTime updatedAt, Long version
) {
}
