package uth.edu.appchat.Models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "private_chats",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"user1_id", "user2_id"})
       })
@Data
public class PrivateChat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user1_id", nullable = false)
    private User user1;

    @ManyToOne
    @JoinColumn(name = "user2_id", nullable = false)
    private User user2;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @OneToMany(mappedBy = "privateChat", cascade = CascadeType.ALL)
    private List<PrivateMessage> messages;

    // Helper methods
    public User getOtherUser(User currentUser) {
        return currentUser.equals(user1) ? user2 : user1;
    }

    public boolean containsUser(User user) {
        return user.equals(user1) || user.equals(user2);
    }
}
