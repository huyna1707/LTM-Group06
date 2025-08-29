package uth.edu.appchat.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uth.edu.appchat.Models.User;
import uth.edu.appchat.Models.UserBlock;
import uth.edu.appchat.Repositories.UserBlockRepository;
import uth.edu.appchat.Repositories.UserRepository;

@Service
@RequiredArgsConstructor
public class UserBlockService {
    private final UserBlockRepository blockRepo;
    private final UserRepository userRepo;

    public boolean isBlocked(Long meId, Long targetId) {
        return blockRepo.existsByBlockerIdAndBlockedId(meId, targetId);
    }

    public boolean isAnyBlockBetween(Long a, Long b) {
        return blockRepo.existsAnyBlockBetween(a, b);
    }

    @Transactional
    public void blockUser(Long meId, Long targetId) {
        if (meId.equals(targetId)) throw new IllegalArgumentException("Không thể tự chặn chính mình.");
        if (blockRepo.existsByBlockerIdAndBlockedId(meId, targetId)) return;
        User me = userRepo.findById(meId).orElseThrow(() -> new RuntimeException("Không tìm thấy bạn"));
        User target = userRepo.findById(targetId).orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        UserBlock b = UserBlock.builder().blocker(me).blocked(target).build();
        blockRepo.save(b);
    }

    @Transactional
    public void unblockUser(Long meId, Long targetId) {
        blockRepo.deleteByBlockerIdAndBlockedId(meId, targetId);
    }
}
