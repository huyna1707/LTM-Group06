package uth.edu.appchat.Models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 32, nullable = false, unique = true)
    private String username;

    @Column(length = 255, nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", length = 72, nullable = false)
    private String passwordHash; // BCrypt

    @Column(length = 100)
    private String fullName;

    @Column(length = 255)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 8)
    private Status status = Status.OFFLINE;

    private LocalDateTime lastSeen;
    private boolean isVerified = false;
    private boolean isBanned = false;

    @PrePersist
    public void prePersist() {
        this.lastSeen = LocalDateTime.now();
    }

    // getters/setters...


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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public boolean isBanned() {
        return isBanned;
    }

    public void setBanned(boolean banned) {
        isBanned = banned;
    }

    public User() {
    }

    public User(Long id, String username, String email, String passwordHash, String fullName, String avatarUrl, Status status, LocalDateTime lastSeen, boolean isVerified, boolean isBanned) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
        this.status = status;
        this.lastSeen = lastSeen;
        this.isVerified = isVerified;
        this.isBanned = isBanned;
    }

    public enum Status { ONLINE, OFFLINE, AWAY, DND }
}