package uth.edu.appchat.Dtos;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class GroupMembershipDTO {
    private String role;     // ADMIN / MODERATOR / MEMBER
    private boolean admin;   // true nếu ADMIN
    private boolean owner;   // true nếu là người tạo nhóm
}