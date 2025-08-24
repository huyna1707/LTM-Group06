package uth.edu.appchat.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import uth.edu.appchat.Dtos.CreateGroupForm;
import uth.edu.appchat.Dtos.GroupDTO;
import uth.edu.appchat.Models.GroupMessage;
import uth.edu.appchat.Dtos.GroupMessageDTO;
import uth.edu.appchat.Dtos.UserDTO;
import uth.edu.appchat.Models.GroupChat;
import uth.edu.appchat.Models.GroupMember;
import uth.edu.appchat.Models.GroupMessage;
import uth.edu.appchat.Models.User;
import uth.edu.appchat.Repositories.GroupChatRepository;
import uth.edu.appchat.Repositories.GroupMemberRepository;
import uth.edu.appchat.Repositories.GroupMessageRepository;
import uth.edu.appchat.Repositories.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupChatService {
    private final GroupChatRepository groupChatRepo;
    private final GroupMemberRepository groupMemberRepo;
    private final GroupMessageRepository groupMessageRepo;
    private final UserRepository userRepo;

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
            member.setRole(user.equals(group.getCreatedBy()) ? GroupMember.GroupRole.ADMIN : GroupMember.GroupRole.MEMBER);
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
        if (!groupMemberRepo.existsByGroupChatIdAndUserIdAndIsActive(groupId, userId, true)) {
            throw new RuntimeException("Bạn không phải thành viên của nhóm này");
        }
        List<GroupMessage> messages = groupMessageRepo.findByGroupChatIdOrderByCreatedAtAsc(groupId);
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
}