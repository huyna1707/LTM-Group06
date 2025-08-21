package uth.edu.appchat.Models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "group_chats")
@Data
public class GroupChat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @OneToMany(mappedBy = "groupChat", cascade = CascadeType.ALL)
    private List<GroupMember> members;

    @OneToMany(mappedBy = "groupChat", cascade = CascadeType.ALL)
    private List<GroupMessage> messages;

    // Helper methods
    public int getMemberCount() {
        return members != null ? members.size() : 0;
    }

    public boolean isValidGroup() {
        return getMemberCount() >= 3;
    }
}
