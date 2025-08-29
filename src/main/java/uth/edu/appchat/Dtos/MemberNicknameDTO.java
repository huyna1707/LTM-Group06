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
}
