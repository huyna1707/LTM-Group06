package uth.edu.appchat.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uth.edu.appchat.Models.GroupChat;
import uth.edu.appchat.Models.PrivateChat;
import uth.edu.appchat.Repositories.GroupChatRepository;
import uth.edu.appchat.Repositories.PrivateChatRepository;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StreakMaintenanceJob {

    private final PrivateChatRepository privateChatRepo;
    private final GroupChatRepository groupChatRepo;

    // Run hourly
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void maintainStreaks() {
        LocalDate today = LocalDate.now();

    // Expire streaks where last date < today.minusDays(2)
    // This gives a window: if user missed exactly 1 day, they can still restore before the 24h after that missed day passes.
    LocalDate cutoff = today.minusDays(2);
        List<PrivateChat> expiredPrivate = privateChatRepo.findByStreakLastDateBefore(cutoff);
        for (PrivateChat pc : expiredPrivate) {
            pc.setStreakCount(0);
            pc.setStreakLastDate(null);
        }
        if (!expiredPrivate.isEmpty()) privateChatRepo.saveAll(expiredPrivate);

        List<GroupChat> expiredGroup = groupChatRepo.findByStreakLastDateBefore(cutoff);
        for (GroupChat gc : expiredGroup) {
            gc.setStreakCount(0);
            gc.setStreakLastDate(null);
        }
        if (!expiredGroup.isEmpty()) groupChatRepo.saveAll(expiredGroup);

        // Reset monthly recovery usage when month/year changed
        int m = today.getMonthValue(); int y = today.getYear();
        List<PrivateChat> needResetP = privateChatRepo.findByRecoveryMonthNot(m, y);
        for (PrivateChat pc : needResetP) {
            pc.setStreakRecoveryUsed(0);
            pc.setStreakRecoveryMonth(m);
            pc.setStreakRecoveryYear(y);
        }
        if (!needResetP.isEmpty()) privateChatRepo.saveAll(needResetP);

        List<GroupChat> needResetG = groupChatRepo.findByRecoveryMonthNot(m, y);
        for (GroupChat gc : needResetG) {
            gc.setStreakRecoveryUsed(0);
            gc.setStreakRecoveryMonth(m);
            gc.setStreakRecoveryYear(y);
        }
        if (!needResetG.isEmpty()) groupChatRepo.saveAll(needResetG);
    }
}
