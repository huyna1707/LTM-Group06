package uth.edu.appchat.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uth.edu.appchat.Dtos.*;
import uth.edu.appchat.Models.GroupChat;
import uth.edu.appchat.Models.GroupMember;
import uth.edu.appchat.Models.GroupMessage;
import uth.edu.appchat.Models.User;
import uth.edu.appchat.Repositories.GroupChatRepository;
import uth.edu.appchat.Repositories.GroupMemberRepository;
import uth.edu.appchat.Repositories.GroupMessageRepository;
import uth.edu.appchat.Repositories.UserRepository;
import uth.edu.appchat.Dtos.AddMembersRequest;
import uth.edu.appchat.Dtos.AddMembersResult;
import java.util.*;
import java.util.stream.Stream;
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
       Tạo nhóm & các hàm đang có
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
            groupMemberRepo.save(member);
        }
    }

    // Services/GroupChatService.java
    public List<GroupDTO> getMyGroups() {
        Long userId = getCurrentUserId();
        List<GroupChat> groups = groupMemberRepo.findActiveGroupsByUserId(userId);
        return groups.stream()
                .map(g -> new GroupDTO(g.getId(), g.getName(), g.getMemberCount(), g.getAvatarUrl()))
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
       Biệt danh thành viên
       ========================= */
    @Transactional(readOnly = true)
    public List<MemberNicknameDTO> getMembersWithNickname(Long groupId) {
        var members = groupMemberRepo.findByGroupIdWithUser(groupId);
        List<MemberNicknameDTO> out = new ArrayList<>();
        for (GroupMember gm : members) {
            User u = gm.getUser();
            out.add(new MemberNicknameDTO(
                    u.getId(),
                    u.getUsername(),
                    u.getFullName(),
                    gm.getNickname()
            ));
        }
        return out;
    }

    @Transactional
    public void saveMemberNicknames(Long groupId, String updaterUsername, List<MemberNicknameDTO> payload) {
        User updater = userRepo.findByUsername(updaterUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

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

    public void clearGroupForMe(Long groupId) {
        Long userId = getCurrentUserId();
        GroupMember me = groupMemberRepo.findByGroupChatIdAndUserId(groupId, userId)
                .orElseThrow(() -> new RuntimeException("Bạn không phải thành viên của nhóm này"));
        me.setClearedAt(LocalDateTime.now());
        groupMemberRepo.save(me);
    }

    /* =========================
       Rời nhóm / Xóa nhóm
       ========================= */

    @Transactional
    public void deleteGroup(Long groupId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        GroupChat group = groupChatRepo.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhóm."));

        boolean isOwner = group.getCreatedBy() != null
                && username.equals(group.getCreatedBy().getUsername());

        boolean isAdmin = groupMemberRepo
                .findByGroupChatIdAndUserUsernameAndIsActiveTrue(groupId, username)
                .map(gm -> gm.getRole() == GroupMember.GroupRole.ADMIN)
                .orElse(false);

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("Bạn không có quyền xóa nhóm này.");
        }

        groupChatRepo.delete(group);
    }
    @Transactional(readOnly = true)
    public GroupMembershipDTO getMyMembership(Long groupId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        var gm = groupMemberRepo
                .findByGroupChatIdAndUserUsernameAndIsActiveTrue(groupId, username)
                .orElseThrow(() -> new IllegalArgumentException("Bạn không còn là thành viên đang hoạt động của nhóm."));

        var group = gm.getGroupChat();
        boolean isOwner = group.getCreatedBy() != null
                && username.equals(group.getCreatedBy().getUsername());

        var role = gm.getRole() != null ? gm.getRole().name() : "MEMBER";
        boolean isAdmin = gm.getRole() == GroupMember.GroupRole.ADMIN;

        return new GroupMembershipDTO(role, isAdmin, isOwner);
    }

    @Transactional
    public AddMembersResult addMembers(Long groupId, AddMembersRequest req) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        GroupChat group = groupChatRepo.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhóm."));

        // Chỉ Owner hoặc Admin mới được thêm
        boolean isOwner = group.getCreatedBy() != null
                && currentUsername.equals(group.getCreatedBy().getUsername());

        boolean isAdmin = groupMemberRepo
                .findByGroupChatIdAndUserUsernameAndIsActiveTrue(groupId, currentUsername)
                .map(gm -> gm.getRole() == GroupMember.GroupRole.ADMIN)
                .orElse(false);

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("Bạn không có quyền thêm thành viên.");
        }

        // Ghép members từ members[] + membersRaw
        Set<String> identifiers = new LinkedHashSet<>();
        if (req.getMembers() != null) identifiers.addAll(req.getMembers());
        if (req.getMembersRaw() != null) {
            Stream.of(req.getMembersRaw().split(","))
                    .map(String::trim).filter(s -> !s.isBlank())
                    .forEach(identifiers::add);
        }

        List<String> added = new ArrayList<>();
        List<String> reactivated = new ArrayList<>();
        List<String> existed = new ArrayList<>();
        List<String> notFound = new ArrayList<>();

        for (String idf : identifiers) {
            // Tìm user theo username/phone/email
            Optional<User> optU = userRepo.findByUsernameOrPhoneOrEmail(idf);
            if (optU.isEmpty()) {
                notFound.add(idf);
                continue;
            }
            User u = optU.get();

            // Không thêm lại chính mình nếu đã là member
            Optional<GroupMember> existedGM = groupMemberRepo.findByGroupChatIdAndUserId(groupId, u.getId());

            if (existedGM.isPresent()) {
                GroupMember gm = existedGM.get();
                if (Boolean.TRUE.equals(gm.getActive())) {
                    existed.add(idf);
                } else {
                    // Reactivate
                    gm.setActive(true);
                    gm.setLeftAt(null);
                    groupMemberRepo.save(gm);
                    reactivated.add(idf);
                }
                continue;
            }

            // Tạo membership mới
            GroupMember gm = new GroupMember();
            gm.setGroupChat(group);
            gm.setUser(u);
            gm.setRole(GroupMember.GroupRole.MEMBER);
            gm.setActive(true);
            groupMemberRepo.save(gm);

            added.add(idf);
        }

        // (Tuỳ chọn) cập nhật lastMessageAt để đẩy nhóm lên đầu danh sách
        group.setLastMessageAt(LocalDateTime.now());
        groupChatRepo.save(group);

        return new AddMembersResult(added, reactivated, existed, notFound);
    }


    @Transactional
    public void leaveGroup(Long groupId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // 1) Lấy membership đang ACTIVE của mình
        GroupMember me = groupMemberRepo
                .findByGroupChatIdAndUserUsernameAndIsActiveTrue(groupId, username)
                .orElseThrow(() -> new IllegalArgumentException("Bạn không đang là thành viên hoạt động của nhóm."));

        GroupChat group = me.getGroupChat();

        // 2) Nếu mình là thành viên cuối cùng -> xoá cả nhóm
        long activeCount = groupMemberRepo.countByGroupChatIdAndIsActiveTrue(groupId);
        if (activeCount == 1) {
            groupChatRepo.delete(group);
            return;
        }

        boolean leavingIsAdmin = me.getRole() == GroupMember.GroupRole.ADMIN;

        // 3) HARD DELETE: xoá hẳn record membership (không set isActive=false nữa)
        groupMemberRepo.delete(me);

        // 4) Nếu người rời là admin và không còn admin nào khác -> promote người vào sớm nhất
        if (leavingIsAdmin) {
            long adminLeft = groupMemberRepo
                    .countByGroupChatIdAndRoleAndIsActiveTrue(groupId, GroupMember.GroupRole.ADMIN);

            if (adminLeft == 0) {
                groupMemberRepo.findFirstByGroupChatIdAndIsActiveTrueOrderByJoinedAtAsc(groupId)
                        .ifPresent(promote -> {
                            promote.setRole(GroupMember.GroupRole.ADMIN);
                            // vì @Transactional nên chỉ cần set role là đủ
                        });
            }
        }
    }
    @Transactional
    public void setGroupAvatar(Long groupId, String byUsername, String url) {
        GroupChat g = groupChatRepo.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhóm."));
        boolean isOwner = g.getCreatedBy() != null && byUsername.equals(g.getCreatedBy().getUsername());
        boolean isAdmin = groupMemberRepo
                .findByGroupChatIdAndUserUsernameAndIsActiveTrue(groupId, byUsername)
                .map(m -> m.getRole() == GroupMember.GroupRole.ADMIN)
                .orElse(false);
        if (!isOwner && !isAdmin) throw new AccessDeniedException("Bạn không có quyền đổi avatar nhóm.");

        g.setAvatarUrl((url == null || url.isBlank()) ? null : url.trim());
        groupChatRepo.save(g);
    }

    @Transactional
    public void clearGroupAvatar(Long groupId, String byUsername) {
        setGroupAvatar(groupId, byUsername, null);
    }
}
