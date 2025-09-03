package uth.edu.appchat.Services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import uth.edu.appchat.Repositories.GroupChatRepository;
import uth.edu.appchat.Repositories.PrivateChatRepository;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class WallpaperService {

    private final PrivateChatRepository privateChatRepo;
    private final GroupChatRepository groupChatRepo;
    private final SimpMessagingTemplate broker;

    @Transactional
    public void clearPrivateWallpaper(Long chatId) {
        privateChatRepo.findById(chatId).ifPresent(chat -> {
            chat.setWallpaperUrl(null);
            privateChatRepo.save(chat);

            // bắn sự kiện realtime cho 2 client
            broker.convertAndSend("/topic/wallpaper/private:" + chatId,
                    Map.of("preset", "none"));
        });
    }

    @Transactional
    public void clearGroupWallpaper(Long groupId) {
        groupChatRepo.findById(groupId).ifPresent(group -> {
            group.setWallpaperUrl(null);
            groupChatRepo.save(group);

            // bắn sự kiện realtime cho cả group
            broker.convertAndSend("/topic/wallpaper/group:" + groupId,
                    Map.of("preset", "none"));
        });
    }
}
