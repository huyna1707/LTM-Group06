package uth.edu.appchat.Dtos;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// UpdateNicknameRequest.java
public record UpdateNicknameRequest(
        @NotBlank @Size(max = 100) String nickname
) {}

