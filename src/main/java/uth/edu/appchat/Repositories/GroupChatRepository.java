package uth.edu.appchat.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uth.edu.appchat.Models.GroupChat;

import java.util.Optional;

@Repository
public interface GroupChatRepository extends JpaRepository<GroupChat, Long> {
    // Tìm nhóm theo tên
    Optional<GroupChat> findByName(String name);
}
