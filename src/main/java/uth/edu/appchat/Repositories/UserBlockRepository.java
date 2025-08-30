package uth.edu.appchat.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uth.edu.appchat.Models.UserBlock;

import java.util.List;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {
    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    @Query("""
           SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
           FROM UserBlock b
           WHERE (b.blocker.id = :a AND b.blocked.id = :b)
              OR (b.blocker.id = :b AND b.blocked.id = :a)
           """)
    boolean existsAnyBlockBetween(Long a, Long b);

    List<UserBlock> findByBlockerId(Long blockerId);

    void deleteByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
}
