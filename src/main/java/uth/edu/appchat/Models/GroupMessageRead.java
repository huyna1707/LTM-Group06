package uth.edu.appchat.Models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "group_message_reads")
@Data
public class GroupMessageRead {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "group_message_id", nullable = false)
    private GroupMessage groupMessage;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "read_at")
    private LocalDateTime readAt = LocalDateTime.now();

    // Unique constraint: một user chỉ đọc một message một lần
    @Table(uniqueConstraints = {
            @UniqueConstraint(columnNames = {"group_message_id", "user_id"})
    })
    public static class Constraints {}
}