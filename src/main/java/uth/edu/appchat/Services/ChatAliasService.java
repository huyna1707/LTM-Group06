// src/main/java/uth/edu/appchat/Services/ChatAliasService.java
package uth.edu.appchat.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import uth.edu.appchat.Models.ChatAlias;
import uth.edu.appchat.Models.User;
import uth.edu.appchat.Repositories.ChatAliasRepository;
import uth.edu.appchat.Repositories.UserRepository;

import static uth.edu.appchat.Models.ChatAlias.ScopeType;

@Service
@RequiredArgsConstructor
public class ChatAliasService {

    private final ChatAliasRepository repo;
    private final UserRepository userRepo;

    private Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User u = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user: " + username));
        return u.getId();
    }

    public String getAlias(ScopeType type, Long scopeId) {
        Long uid = getCurrentUserId();
        return repo.findByUserIdAndScopeTypeAndScopeId(uid, type, scopeId)
                .map(ChatAlias::getTitleAlias).orElse(null);
    }

    public String upsertAlias(ScopeType type, Long scopeId, String alias) {
        Long uid = getCurrentUserId();
        ChatAlias ca = repo.findByUserIdAndScopeTypeAndScopeId(uid, type, scopeId)
                .orElseGet(() -> ChatAlias.builder()
                        .user(userRepo.findById(uid).orElseThrow())
                        .scopeType(type)
                        .scopeId(scopeId)
                        .build());
        ca.setTitleAlias((alias != null && !alias.isBlank()) ? alias.trim() : null);
        repo.save(ca);
        return ca.getTitleAlias();
    }
}
