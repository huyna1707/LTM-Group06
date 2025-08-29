package uth.edu.appchat.Models;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name="friends",
        uniqueConstraints=@UniqueConstraint(name="uk_friend_pair", columnNames={"user_id","friend_id"}),
        indexes = {
                @Index(name="idx_friend_user", columnList="user_id"),
                @Index(name="idx_friend_friend", columnList="friend_id"),
                @Index(name="idx_friend_status", columnList="status")
        })
@Check(constraints="user_id <> friend_id")
@Data
public class Friend {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="user_id", nullable=false,
            foreignKey = @ForeignKey(name="fk_friend_user"))
    private User user;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="friend_id", nullable=false,
            foreignKey = @ForeignKey(name="fk_friend_friend"))
    private User friend;

    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable=false, length=10)
    private FriendStatus status = FriendStatus.PENDING;

    @CreationTimestamp
    @Column(name="created_at", nullable=false, updatable=false)
    private LocalDateTime createdAt;

    public enum FriendStatus { PENDING, ACCEPTED, BLOCKED }
}
