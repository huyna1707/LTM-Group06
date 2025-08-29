package uth.edu.appchat.Models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name="users",
        indexes = {
                @Index(name="idx_user_username", columnList="username"),
                @Index(name="idx_user_email", columnList="email"),
                @Index(name="idx_user_last_seen", columnList="last_seen")
        })
public class User {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(name="username", nullable=false, unique=true, length=32)
    private String username;

    @Column(name="email", nullable=false, unique=true, length=255)
    private String email;

    @Column(length = 20, unique = true)
    private String phone;

    @Column(name="password_hash", nullable=false, length=72)
    private String passwordHash;

    @Column(name="full_name", length=100)
    private String fullName;

    @Column(name="avatar_url", length=500)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable=false, length=10)
    private UserStatus status = UserStatus.OFFLINE;

    @Column(name="last_seen")
    private LocalDateTime lastSeen;

    public enum UserStatus { ONLINE, OFFLINE, AWAY, DND }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
