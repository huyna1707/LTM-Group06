package uth.edu.appchat.Models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_blocks",
        uniqueConstraints = @UniqueConstraint(name="uk_block_pair", columnNames = {"blocker_id","blocked_id"}),
        indexes = {
                @Index(name="idx_block_blocker", columnList = "blocker_id"),
                @Index(name="idx_block_blocked", columnList = "blocked_id"),
                @Index(name="idx_block_created", columnList = "created_at")
        })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class UserBlock {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Người thực hiện chặn
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="blocker_id", nullable=false, foreignKey=@ForeignKey(name="fk_block_blocker"))
    private User blocker;

    // Người bị chặn
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="blocked_id", nullable=false, foreignKey=@ForeignKey(name="fk_block_blocked"))
    private User blocked;

    @CreationTimestamp
    @Column(name="created_at", nullable=false, updatable=false)
    private LocalDateTime createdAt;
}
