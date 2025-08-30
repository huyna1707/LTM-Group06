package uth.edu.appchat.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data @AllArgsConstructor
public class AddMembersResult {
    private List<String> added;       // thêm mới
    private List<String> reactivated; // từng rời nhóm, kích hoạt lại
    private List<String> existed;     // đã ở trong nhóm
    private List<String> notFound;    // không tìm thấy user
}