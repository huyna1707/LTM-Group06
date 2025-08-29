package uth.edu.appchat.Models;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs",
        indexes = {
                @Index(name="idx_activity_user", columnList="user_id"),
                @Index(name="idx_activity_created", columnList="created_at")
        })
@Data
public class ActivityLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name="fk_activity_user"))
    private User user;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}