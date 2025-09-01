package uth.edu.appchat.Controllers;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import uth.edu.appchat.Dtos.NicknameResponse;
import uth.edu.appchat.Dtos.UpdateNicknameRequest;
import uth.edu.appchat.Dtos.UserDTO;
import uth.edu.appchat.Repositories.PrivateChatRepository;
import uth.edu.appchat.Repositories.UserRepository;
import uth.edu.appchat.Services.ChatNicknameService;
import uth.edu.appchat.Services.GroupChatService;
import uth.edu.appchat.Services.PrivateChatQueryService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class NicknameController {

    private final ChatNicknameService service;
    private final UserRepository userRepo;
    private final PrivateChatRepository privateRepo;
    private final PrivateChatQueryService queryService;


    public NicknameController(ChatNicknameService service, UserRepository userRepo,
                              PrivateChatRepository privateRepo, PrivateChatQueryService queryService) {
        this.service = service;
        this.userRepo = userRepo;
        this.privateRepo = privateRepo;
        this.queryService = queryService;
    }

    private Long resolveUserId(Authentication auth) {
        String key = auth.getName();
        return userRepo.findByUsernameOrPhoneOrEmail(key)
                .map(u -> u.getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + key));
    }

    // --- GROUP
    @PatchMapping("/groups/{id}/nickname")
    public NicknameResponse updateGroup(@PathVariable Long id,
                                        @Valid @RequestBody UpdateNicknameRequest req,
                                        Authentication auth) {
        Long userId = resolveUserId(auth);
        return service.updateGroup(id, req.nickname(), userId);
    }



    // --- PRIVATE (PATCH)
    @PatchMapping("/private-chats/{id}/nickname")
    public NicknameResponse updatePrivate(@PathVariable Long id,
                                          @Valid @RequestBody UpdateNicknameRequest req,
                                          Authentication auth) {
        Long userId = resolveUserId(auth);
        return service.updatePrivate(id, req.nickname(), userId);
    }

    // --- PRIVATE (GET nickname để hydrate khi reload)
    @GetMapping("/private-chats/{id}/nickname")
    public NicknameResponse getPrivateNickname(@PathVariable Long id, Authentication auth) {
        Long currentUserId = resolveUserId(auth);
        return queryService.getPartnerWithNickname(id, currentUserId);
    }
}