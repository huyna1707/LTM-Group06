package uth.edu.appchat.Services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uth.edu.appchat.Models.ChatClear;
import uth.edu.appchat.Models.PrivateChat;
import uth.edu.appchat.Repositories.ChatClearRepository;
import uth.edu.appchat.Repositories.PrivateChatRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PrivateChatService {

    private final PrivateChatRepository privateChatRepo;
    private final ChatClearRepository chatClearRepo;

    /**
     * Xóa đoạn chat CHỈ Ở PHÍA userId:
     * - Không xóa dữ liệu tin nhắn trong DB
     * - Người kia vẫn thấy lịch sử bình thường
     * - Lần sau load lịch sử, backend sẽ chỉ trả các tin sau thời điểm clearedAt này
     */
    @Transactional
    public void clearChatForMe(Long chatId, Long userId) {
        // 1) Kiểm tra chat tồn tại và user là thành viên
        PrivateChat chat = privateChatRepo.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat không tồn tại"));

        boolean isMember = chat.getUser1().getId().equals(userId)
                || chat.getUser2().getId().equals(userId);
        if (!isMember) {
            throw new RuntimeException("Bạn không thuộc đoạn chat này");
        }

        // 2) Ghi/ cập nhật mốc clearedAt cho (userId, 'private', chatId)
        ChatClear.ChatClearId id = new ChatClear.ChatClearId(userId, "private", chatId);

        ChatClear rec = chatClearRepo.findById(id)
                .orElseGet(() -> new ChatClear(id, LocalDateTime.now()));
        rec.setClearedAt(LocalDateTime.now());

        chatClearRepo.save(rec);
    }
}
