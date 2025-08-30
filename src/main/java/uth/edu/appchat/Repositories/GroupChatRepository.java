package uth.edu.appchat.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import uth.edu.appchat.Models.GroupChat;

public interface GroupChatRepository extends JpaRepository<GroupChat, Long> {
    // Bạn có thể thêm các hàm custom nếu cần tìm kiếm nhóm theo tên, creator, v.v.
    @org.springframework.data.jpa.repository.Query("SELECT g FROM GroupChat g WHERE g.streakLastDate IS NOT NULL AND g.streakLastDate < :cutoff")
    java.util.List<uth.edu.appchat.Models.GroupChat> findByStreakLastDateBefore(@org.springframework.data.repository.query.Param("cutoff") java.time.LocalDate cutoff);

    @org.springframework.data.jpa.repository.Query("SELECT g FROM GroupChat g WHERE g.streakRecoveryMonth IS NULL OR g.streakRecoveryYear IS NULL OR g.streakRecoveryMonth <> :month OR g.streakRecoveryYear <> :year")
    java.util.List<uth.edu.appchat.Models.GroupChat> findByRecoveryMonthNot(@org.springframework.data.repository.query.Param("month") Integer month, @org.springframework.data.repository.query.Param("year") Integer year);
}
