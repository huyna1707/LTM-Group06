package uth.edu.appchat.Dtos;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank(message = "Tên đăng nhập hoặc email không được để trống")
    private String usernameOrEmail;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;

    public LoginRequest() {}

    public LoginRequest(String usernameOrEmail, String password) {
        this.usernameOrEmail = usernameOrEmail;
        this.password = password;
    }

    // Getters and setters
    public String getUsernameOrEmail() {
        return usernameOrEmail;
    }

    public void setUsernameOrEmail(String usernameOrEmail) {
        this.usernameOrEmail = usernameOrEmail;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
