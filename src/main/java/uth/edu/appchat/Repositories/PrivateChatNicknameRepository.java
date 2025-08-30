package uth.edu.appchat.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uth.edu.appchat.Models.PrivateChatNickname;

import java.util.Optional;
import java.util.List;

@Repository
public interface PrivateChatNicknameRepository extends JpaRepository<PrivateChatNickname, Long> {
    Optional<PrivateChatNickname> findByChat_IdAndOwner_IdAndTarget_Id(Long chatId, Long ownerId, Long targetId);
    List<PrivateChatNickname> findByChat_IdAndOwner_Id(Long chatId, Long ownerId);
}