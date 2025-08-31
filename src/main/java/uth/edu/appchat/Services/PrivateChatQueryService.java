package uth.edu.appchat.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uth.edu.appchat.Dtos.NicknameResponse;
import uth.edu.appchat.Models.PrivateChat;
import uth.edu.appchat.Models.User;
import uth.edu.appchat.Repositories.PrivateChatNicknameRepository;
import uth.edu.appchat.Repositories.PrivateChatRepository;
import uth.edu.appchat.Repositories.UserRepository;

@Service
@RequiredArgsConstructor
public class PrivateChatQueryService {

    private final PrivateChatRepository privateRepo;
    private final UserRepository userRepo;
    private final PrivateChatNicknameRepository privNickRepo; // Sử dụng repository chuyên dụng

    /**
     * Lấy thông tin đối tác trong private chat kèm biệt danh (nếu có).
     */
    @Transactional(readOnly = true)
    public NicknameResponse getPartnerWithNickname(Long chatId, Long currentUserId) {
        PrivateChat chat = privateRepo.findWithUsersById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chat #" + chatId));

        User currentUser = userRepo.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user #" + currentUserId));

        if (!chat.containsUser(currentUser)) {
            throw new IllegalArgumentException("Bạn không có quyền truy cập chat này.");
        }

        // Xác định partner
        User partner = chat.getOtherUser(currentUser);

        // Lấy biệt danh cá nhân hóa từ PrivateChatNickname (nếu có)
        String nickname = null; // Mặc định không có biệt danh
        var nickRecord = privNickRepo.findByChat_IdAndOwner_IdAndTarget_Id(chatId, currentUserId, partner.getId())
                .orElse(null);
        if (nickRecord != null && nickRecord.getNickname() != null) {
            nickname = nickRecord.getNickname();
        }

        return new NicknameResponse(
                chat.getId(),
                "private",
                nickname,
                nickRecord != null ? currentUserId : null, // Ai đặt biệt danh (nếu có)
                nickRecord != null ? nickRecord.getUpdatedAt() : null,
                chat.getVersion()
        );
    }
}