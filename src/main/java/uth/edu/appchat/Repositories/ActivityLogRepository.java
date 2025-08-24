package uth.edu.appchat.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uth.edu.appchat.Models.ActivityLog;
import uth.edu.appchat.Models.User;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    // Tìm log theo user
    List<ActivityLog> findByUser(User user);

    // Tìm log theo action
    List<ActivityLog> findByAction(String action);

    // Tìm log của user trong khoảng thời gian
    @Query("SELECT al FROM ActivityLog al WHERE al.user = :user AND al.createdAt BETWEEN :startDate AND :endDate ORDER BY al.createdAt DESC")
    List<ActivityLog> findByUserAndDateRange(@Param("user") User user,
                                             @Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    // Lấy log gần nhất của user
    @Query("SELECT al FROM ActivityLog al WHERE al.user = :user ORDER BY al.createdAt DESC LIMIT 10")
    List<ActivityLog> findRecentActivities(@Param("user") User user);
}