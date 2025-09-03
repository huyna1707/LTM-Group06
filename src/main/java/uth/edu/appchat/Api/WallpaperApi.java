package uth.edu.appchat.Api;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uth.edu.appchat.Models.GroupChat;
import uth.edu.appchat.Models.PrivateChat;
import uth.edu.appchat.Repositories.GroupChatRepository;
import uth.edu.appchat.Repositories.PrivateChatRepository;
import uth.edu.appchat.Services.StorageService;
import uth.edu.appchat.Services.WallpaperService;

import java.io.IOException;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/wallpaper")
public class WallpaperApi {

    private final SimpMessagingTemplate broker;
    private final GroupChatRepository groupRepo;
    private final PrivateChatRepository privateRepo; // nếu dùng private chat ID
    private final StorageService storage;
    private final WallpaperService wallpaperService;

    // ---------- GROUP ----------
    @PostMapping(value="/group/{groupId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public Map<String, String> setGroupWallpaper(@PathVariable Long groupId,
                                                 @RequestPart("file") MultipartFile file) throws IOException {
        // TODO: kiểm tra quyền thành viên nhóm / role
        String url = storage.saveWallpaper(file);
        GroupChat gc = groupRepo.findById(groupId).orElseThrow();
        gc.setWallpaperUrl(url);
        // phát realtime tới tất cả client trong nhóm
        broker.convertAndSend("/topic/wallpaper/group:" + groupId, Map.of("url", url));
        return Map.of("url", url);
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<?> getGroupWallpaper(@PathVariable Long groupId) {
        return groupRepo.findById(groupId)
                .map(gc -> {
                    if (gc.getWallpaperUrl()==null || gc.getWallpaperUrl().isBlank())
                        return ResponseEntity.noContent().build();
                    return ResponseEntity.ok(Map.of("url", gc.getWallpaperUrl()));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ---------- PRIVATE ----------
    @PostMapping(value="/private/{chatId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public Map<String, String> setPrivateWallpaper(@PathVariable Long chatId,
                                                   @RequestPart("file") MultipartFile file) throws IOException {
        // TODO: kiểm tra người dùng có thuộc privateChat này
        String url = storage.saveWallpaper(file);
        PrivateChat pc = privateRepo.findById(chatId).orElseThrow();
        pc.setWallpaperUrl(url);
        broker.convertAndSend("/topic/wallpaper/private:" + chatId, Map.of("url", url));
        return Map.of("url", url);
    }

    @GetMapping("/private/{chatId}")
    public ResponseEntity<?> getPrivateWallpaper(@PathVariable Long chatId) {
        return privateRepo.findById(chatId)
                .map(pc -> {
                    if (pc.getWallpaperUrl()==null || pc.getWallpaperUrl().isBlank())
                        return ResponseEntity.noContent().build();
                    return ResponseEntity.ok(Map.of("url", pc.getWallpaperUrl()));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/private/{chatId}")
    public ResponseEntity<?> clearPrivateWallpaper(@PathVariable Long chatId) {
        wallpaperService.clearPrivateWallpaper(chatId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/group/{groupId}")
    public ResponseEntity<?> clearGroupWallpaper(@PathVariable Long groupId) {
        wallpaperService.clearGroupWallpaper(groupId);
        return ResponseEntity.noContent().build();
    }


}

