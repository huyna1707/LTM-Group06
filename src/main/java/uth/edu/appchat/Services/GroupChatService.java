package uth.edu.appchat.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uth.edu.appchat.Dtos.CreateGroupForm;
import uth.edu.appchat.Dtos.GroupDTO;
import uth.edu.appchat.Dtos.GroupMessageDTO;
import uth.edu.appchat.Dtos.UserDTO;
import uth.edu.appchat.Dtos.MemberNicknameDTO;

import uth.edu.appchat.Models.GroupChat;
import uth.edu.appchat.Models.GroupMember;
import uth.edu.appchat.Models.GroupMessage;
import uth.edu.appchat.Models.User;

import uth.edu.appchat.Repositories.GroupChatRepository;
import uth.edu.appchat.Repositories.GroupMemberRepository;
import uth.edu.appchat.Repositories.GroupMessageRepository;
import uth.edu.appchat.Repositories.UserRepository;
import uth.edu.appchat.Repositories.UserBlockRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupChatService {
    private final GroupChatRepository groupChatRepo;
    private final GroupMemberRepository groupMemberRepo;
    private final GroupMessageRepository groupMessageRepo;
    private final UserRepository userRepo;

    /* =========================
       Các hàm cũ giữ nguyên
       ========================= */
    public GroupChat createGroup(CreateGroupForm form, User creator) {
        GroupChat group = new GroupChat();
        group.setName(form.getName());
        group.setCreatedBy(creator);
        group = groupChatRepo.save(group);

        addMember(group, creator);
        for (String identifier : form.getMembers()) {
            User user = userRepo.findByUsernameOrPhoneOrEmail(identifier)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + identifier));
            addMember(group, user);
        }
        return group;
    }

    private void addMember(GroupChat group, User user) {
        if (!groupMemberRepo.existsByGroupChatIdAndUserId(group.getId(), user.getId())) {
            GroupMember member = new GroupMember();
            member.setGroupChat(group);
            member.setUser(user);
            member.setRole(user.equals(group.getCreatedBy())
                    ? GroupMember.GroupRole.ADMIN
                    : GroupMember.GroupRole.MEMBER);
            // nếu entity có cờ isActive/joinedAt, bạn có thể set ở đây
            groupMemberRepo.save(member);
        }
    }

    public List<GroupDTO> getMyGroups() {
        Long userId = getCurrentUserId();
        List<GroupChat> groups = groupMemberRepo.findActiveGroupsByUserId(userId);
        return groups.stream()
                .map(group -> new GroupDTO(group.getId(), group.getName(), group.getMemberCount()))
                .collect(Collectors.toList());
    }

    public List<GroupMessageDTO> getGroupMessages(Long groupId) {
        Long userId = getCurrentUserId();

        GroupMember me = groupMemberRepo.findByGroupChatIdAndUserId(groupId, userId)
                .orElseThrow(() -> new RuntimeException("Bạn không phải thành viên của nhóm này"));

        LocalDateTime cutoff = me.getClearedAt();

        List<GroupMessage> messages = groupMessageRepo.findByGroupChatIdOrderByCreatedAtAsc(groupId);
        if (cutoff != null) {
            messages = messages.stream()
                    .filter(m -> m.getCreatedAt().isAfter(cutoff))
                    .collect(Collectors.toList());
        }

        return messages.stream()
                .map(msg -> new GroupMessageDTO(
                        msg.getId(),
                        groupId,
                        new UserDTO(msg.getSender().getId(), msg.getSender().getUsername(), msg.getSender().getFullName()),
                        msg.getContent(),
                        msg.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }


    public GroupMessageDTO sendGroupMessage(Long groupId, String content) {
        Long userId = getCurrentUserId();
        if (!groupMemberRepo.existsByGroupChatIdAndUserIdAndIsActive(groupId, userId, true)) {
            throw new RuntimeException("Bạn không phải thành viên của nhóm này");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("Nội dung tin nhắn không được để trống");
        }
        User sender = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        GroupChat group = groupChatRepo.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhóm"));
        GroupMessage message = new GroupMessage();
        message.setGroupChat(group);
        message.setSender(sender);
        message.setContent(content.trim());
        message = groupMessageRepo.save(message);

        group.setLastMessageAt(LocalDateTime.now());
        groupChatRepo.save(group);

        return new GroupMessageDTO(
                message.getId(),
                groupId,
                new UserDTO(sender.getId(), sender.getUsername(), sender.getFullName()),
                message.getContent(),
                message.getCreatedAt()
        );
    }

    private Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + username));
        return user.getId();
    }

    /* =========================
       THÊM MỚI CHO "BIỆT DANH"
       ========================= */

    // GET /api/groups/{groupId}/members-with-nickname
    @Transactional(readOnly = true)
    public List<MemberNicknameDTO> getMembersWithNickname(Long groupId) {
        var members = groupMemberRepo.findByGroupIdWithUser(groupId); // <- cần method này ở repo
        List<MemberNicknameDTO> out = new ArrayList<>();
        for (GroupMember gm : members) {
            User u = gm.getUser();
            out.add(new MemberNicknameDTO(
                    u.getId(),
                    u.getUsername(),
                    u.getFullName(),
                    gm.getNickname() // field đã thêm trong GroupMember
            ));
        }
        return out;
    }

    // POST /api/groups/{groupId}/nicknames
    @Transactional
    public void saveMemberNicknames(Long groupId, String updaterUsername, List<MemberNicknameDTO> payload) {
        User updater = userRepo.findByUsername(updaterUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Chỉ cho phép thành viên nhóm cập nhật
        boolean isMember = groupMemberRepo.existsByGroupChatIdAndUserId(groupId, updater.getId());
        if (!isMember) throw new AccessDeniedException("Bạn không thuộc nhóm này");

        for (MemberNicknameDTO dto : payload) {
            groupMemberRepo.findByGroupChatIdAndUserId(groupId, dto.getUserId())
                    .ifPresent(gm -> {
                        String nn = (dto.getNickname() == null || dto.getNickname().isBlank())
                                ? null : dto.getNickname().trim();
                        gm.setNickname(nn);
                        gm.setNicknameUpdatedBy(updater.getId());
                        gm.setNicknameUpdatedAt(LocalDateTime.now());
                    });
        }
    }

    private final UserBlockRepository blockRepo;
    public void clearGroupForMe(Long groupId) {
        Long userId = getCurrentUserId();
        GroupMember me = groupMemberRepo.findByGroupChatIdAndUserId(groupId, userId)
                .orElseThrow(() -> new RuntimeException("Bạn không phải thành viên của nhóm này"));
        me.setClearedAt(LocalDateTime.now());
        groupMemberRepo.save(me);
    }

    



}
