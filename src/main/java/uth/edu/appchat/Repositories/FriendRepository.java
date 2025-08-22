package uth.edu.appchat.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uth.edu.appchat.Models.Friend;
import uth.edu.appchat.Models.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRepository extends JpaRepository<Friend, Long> {

    // Tìm quan hệ bạn bè giữa hai người
    @Query("SELECT f FROM Friend f WHERE (f.user = :user1 AND f.friend = :user2) OR (f.user = :user2 AND f.friend = :user1)")
    Optional<Friend> findFriendshipBetween(@Param("user1") User user1, @Param("user2") User user2);

    // Lấy danh sách bạn bè đã chấp nhận
    @Query("SELECT f FROM Friend f WHERE (f.user = :user OR f.friend = :user) AND f.status = 'ACCEPTED'")
    List<Friend> findAcceptedFriends(@Param("user") User user);

    // Lấy danh sách lời mời kết bạn đang chờ
    @Query("SELECT f FROM Friend f WHERE f.friend = :user AND f.status = 'PENDING'")
    List<Friend> findPendingFriendRequests(@Param("user") User user);

    // Lấy danh sách đã gửi lời mời
    @Query("SELECT f FROM Friend f WHERE f.user = :user AND f.status = 'PENDING'")
    List<Friend> findSentFriendRequests(@Param("user") User user);
}