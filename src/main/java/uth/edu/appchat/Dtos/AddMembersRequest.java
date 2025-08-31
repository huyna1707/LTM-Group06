package uth.edu.appchat.Dtos;

import lombok.Data;
import java.util.List;

@Data
public class AddMembersRequest {
    // Cho phép FE gửi mảng identifiers hoặc chuỗi “a,b,c”
    private List<String> members;
    private String membersRaw;
}