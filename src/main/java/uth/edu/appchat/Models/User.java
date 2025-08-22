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
}
