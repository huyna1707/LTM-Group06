package uth.edu.appchat.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uth.edu.appchat.Dtos.CreateGroupForm;
import uth.edu.appchat.Models.GroupChat;
import uth.edu.appchat.Models.GroupMember;
import uth.edu.appchat.Models.User;
import uth.edu.appchat.Repositories.GroupChatRepository;
import uth.edu.appchat.Repositories.GroupMemberRepository;
import uth.edu.appchat.Repositories.UserRepository;

@Service
@RequiredArgsConstructor
public class GroupChatService {
    private final GroupChatRepository groupChatRepo;
    private final GroupMemberRepository groupMemberRepo;
    private final UserRepository userRepo;

    public GroupChat createGroup(CreateGroupForm form, User creator) {
        GroupChat group = new GroupChat();
        group.setName(form.getName());
        group.setCreatedBy(creator);
        group = groupChatRepo.save(group);

        // Thêm người tạo vào nhóm
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
            groupMemberRepo.save(member);
        }
    }
}
