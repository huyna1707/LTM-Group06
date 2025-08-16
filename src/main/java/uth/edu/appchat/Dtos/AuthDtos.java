package uth.edu.appchat.Dtos;

import jakarta.validation.constraints.*;

public class AuthDtos {

    public static class RegisterRequest {
        @NotBlank @Size(min=3, max=32)
        private String username;

        @NotBlank @Email @Size(max=255)
        private String email;

        @NotBlank @Size(min=8, max=72)
        private String password;

        @Size(max=100)
        private String fullName;

        // Constructors
        public RegisterRequest() {}

        public RegisterRequest(String username, String email, String password, String fullName) {
            this.username = username;
            this.email = email;
            this.password = password;
            this.fullName = fullName;
        }

        // Getters and Setters
        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }
    }

    public static class LoginRequest {
        @NotBlank
        private String usernameOrEmail;

        @NotBlank
        private String password;

        // Constructors
        public LoginRequest() {}

        public LoginRequest(String usernameOrEmail, String password) {
            this.usernameOrEmail = usernameOrEmail;
            this.password = password;
        }

        // Getters and Setters
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

    public static class AuthResponse {
        public String accessToken;
        public String tokenType = "Bearer";
        public String username;

        public AuthResponse(String accessToken, String username) {
            this.accessToken = accessToken;
            this.username = username;
        }
    }
}
