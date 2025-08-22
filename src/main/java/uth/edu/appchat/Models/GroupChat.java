package uth.edu.appchat.Models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "group_chats",
        indexes = {
                @Index(name="idx_groupchat_creator", columnList="created_by"),
                @Index(name="idx_groupchat_created", columnList="created_at"),
                @Index(name="idx_groupchat_lastmsg", columnList="last_message_at")
        })
@Data
public class GroupChat {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(name="name", nullable=false, length=100)
    private String name;

    @Column(name="description", length=500)
    private String description;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="created_by", nullable=false,
            foreignKey=@ForeignKey(name="fk_groupchat_creator"))
    private User createdBy;

    @CreationTimestamp
    @Column(name="created_at", nullable=false, updatable=false)
    private LocalDateTime createdAt;

    @Column(name="last_message_at")
    private LocalDateTime lastMessageAt;

    @OneToMany(mappedBy="groupChat", cascade=CascadeType.ALL, orphanRemoval=true, fetch=FetchType.LAZY)
    @OrderBy("joinedAt ASC")
    @JsonIgnore
    private List<GroupMember> members;

    @OneToMany(mappedBy="groupChat", cascade=CascadeType.ALL, orphanRemoval=true, fetch=FetchType.LAZY)
    @OrderBy("createdAt ASC")
    @JsonIgnore
    private List<GroupMessage> messages;

    public int getMemberCount() { return members != null ? members.size() : 0; }
    public boolean isValidGroup() { return getMemberCount() >= 3; }
}
