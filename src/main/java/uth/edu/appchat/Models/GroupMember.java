package uth.edu.appchat.Models;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name="group_members",
        uniqueConstraints=@UniqueConstraint(name="uk_group_user_active", columnNames={"group_chat_id","user_id","is_active"}),
        indexes = {
                @Index(name="idx_gm_group", columnList="group_chat_id"),
                @Index(name="idx_gm_user", columnList="user_id"),
                @Index(name="idx_gm_role", columnList="role")
        })
@Data
public class GroupMember {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="group_chat_id", nullable=false,
            foreignKey=@ForeignKey(name="fk_gm_group"))
    private GroupChat groupChat;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="user_id", nullable=false,
            foreignKey=@ForeignKey(name="fk_gm_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name="role", nullable=false, length=10)
    private GroupRole role = GroupRole.MEMBER;

    @CreationTimestamp
    @Column(name="joined_at", nullable=false, updatable=false)
    private LocalDateTime joinedAt;

    @Column(name="left_at")
    private LocalDateTime leftAt;

    @Column(name="is_active", nullable=false)
    private Boolean isActive = true;

    public enum GroupRole { ADMIN, MODERATOR, MEMBER }

    @Column(name = "nickname", length = 100)
    private String nickname;

    @Column(name = "nickname_updated_by")
    private Long nicknameUpdatedBy;

    @Column(name = "nickname_updated_at")
    private LocalDateTime nicknameUpdatedAt;

    @Column(name = "cleared_at")
    private LocalDateTime clearedAt;

    public LocalDateTime getClearedAt() { return clearedAt; }
    public void setClearedAt(LocalDateTime t) { this.clearedAt = t; }
}
