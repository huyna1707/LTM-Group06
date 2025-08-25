package uth.edu.appchat.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uth.edu.appchat.Models.User;
import uth.edu.appchat.Repositories.UserRepository;
import uth.edu.appchat.Services.UserBlockService;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/blocks")
@RequiredArgsConstructor
public class BlockController {
    private final UserBlockService blockService;
    private final UserRepository userRepo;

    private Long currentUserId(Principal principal) {
        User me = userRepo.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        return me.getId();
    }

    @GetMapping("/users/{targetId}/status")
    public Map<String, Boolean> status(@PathVariable Long targetId, Principal p) {
        Long meId = currentUserId(p);
        return Map.of("blocked", blockService.isBlocked(meId, targetId));
    }

    @PostMapping("/users/{targetId}")
    public ResponseEntity<?> block(@PathVariable Long targetId, Principal p) {
        blockService.blockUser(currentUserId(p), targetId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/users/{targetId}")
    public ResponseEntity<?> unblock(@PathVariable Long targetId, Principal p) {
        blockService.unblockUser(currentUserId(p), targetId);
        return ResponseEntity.ok().build();
    }
}
