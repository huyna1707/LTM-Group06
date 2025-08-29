package uth.edu.appchat.Controllers;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import uth.edu.appchat.Services.ChatNicknameService;
import uth.edu.appchat.Repositories.UserRepository;
import uth.edu.appchat.Dtos.NicknameResponse;
import uth.edu.appchat.Dtos.UpdateNicknameRequest;

@RestController
@RequestMapping("/api")
public class NicknameController {

    private final ChatNicknameService service;
    private final UserRepository userRepo;

    public NicknameController(ChatNicknameService service, UserRepository userRepo) {
        this.service = service;
        this.userRepo = userRepo;
    }

    // --- helper: lấy userId từ auth.getName() (username/phone/email đều được)
    private Long resolveUserId(Authentication auth) {
        String value = auth.getName();
        return userRepo.findByUsernameOrPhoneOrEmail(value)
                .map(u -> u.getId())
                .orElseThrow(() -> new RuntimeException("User not found: " + value));
    }

    @PatchMapping("/groups/{id}/nickname")
    public NicknameResponse updateGroup(@PathVariable Long id,
                                        @Valid @RequestBody UpdateNicknameRequest req,
                                        Authentication auth) {
        Long userId = resolveUserId(auth);
        return service.updateGroup(id, req.nickname(), userId);
    }

    @PatchMapping("/private-chats/{id}/nickname")
    public NicknameResponse updatePrivate(@PathVariable Long id,
                                          @Valid @RequestBody UpdateNicknameRequest req,
                                          Authentication auth) {
        Long userId = resolveUserId(auth);
        return service.updatePrivate(id, req.nickname(), userId);
    }
}
