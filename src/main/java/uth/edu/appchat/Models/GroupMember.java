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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public GroupChat getGroupChat() {
        return groupChat;
    }

    public void setGroupChat(GroupChat groupChat) {
        this.groupChat = groupChat;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public GroupRole getRole() {
        return role;
    }

    public void setRole(GroupRole role) {
        this.role = role;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public LocalDateTime getLeftAt() {
        return leftAt;
    }

    public void setLeftAt(LocalDateTime leftAt) {
        this.leftAt = leftAt;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Long getNicknameUpdatedBy() {
        return nicknameUpdatedBy;
    }

    public void setNicknameUpdatedBy(Long nicknameUpdatedBy) {
        this.nicknameUpdatedBy = nicknameUpdatedBy;
    }

    public LocalDateTime getNicknameUpdatedAt() {
        return nicknameUpdatedAt;
    }

    public void setNicknameUpdatedAt(LocalDateTime nicknameUpdatedAt) {
        this.nicknameUpdatedAt = nicknameUpdatedAt;
    }
}
