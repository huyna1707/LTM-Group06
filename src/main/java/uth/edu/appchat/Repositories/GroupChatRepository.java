package uth.edu.appchat.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import uth.edu.appchat.Models.GroupChat;

public interface GroupChatRepository extends JpaRepository<GroupChat, Long> {
    // Bạn có thể thêm các hàm custom nếu cần tìm kiếm nhóm theo tên, creator, v.v.
}
