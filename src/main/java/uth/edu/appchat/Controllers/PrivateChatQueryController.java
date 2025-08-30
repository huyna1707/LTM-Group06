package uth.edu.appchat.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import uth.edu.appchat.Dtos.NicknameResponse;
import uth.edu.appchat.Repositories.UserRepository;
import uth.edu.appchat.Services.PrivateChatQueryService;

@RestController
@RequestMapping("/api/private-chat")
@RequiredArgsConstructor
public class PrivateChatQueryController {

    private final PrivateChatQueryService queryService;
    private final UserRepository userRepo;

    private Long resolveUserId(Authentication auth) {
        String value = auth.getName();
        return userRepo.findByUsernameOrPhoneOrEmail(value)
                .map(u -> u.getId())
                .orElseThrow(() -> new RuntimeException("User not found: " + value));
    }

    /**
     * GET /api/private-chat/{id}/partner-with-nickname
     */
    @GetMapping("/{id}/partner-with-nickname")
    public NicknameResponse getPartnerWithNickname(@PathVariable Long id, Authentication auth) {
        Long currentUserId = resolveUserId(auth);
        return queryService.getPartnerWithNickname(id, currentUserId);
    }
}