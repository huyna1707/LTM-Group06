package uth.edu.appchat.Dtos;

import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private String fullName;

    public UserDTO(Long id, String username, String fullName) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
    }
}