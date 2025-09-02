package uth.edu.appchat.Dtos;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberNicknameDTO {
    private Long userId;     // id của thành viên trong group
    private String username; // username để hiển thị/search
    private String fullName; // tên đầy đủ
    private String nickname; // biệt danh hiện tại trong group (có thể null)


    private String role;      // ADMIN / MEMBER / MODERATOR
    private boolean owner;    // true nếu là người tạo nhóm
    private boolean admin;    // true nếu ADMIN
}
