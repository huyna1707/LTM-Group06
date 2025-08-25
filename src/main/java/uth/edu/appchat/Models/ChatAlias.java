// src/main/java/uth/edu/appchat/Models/ChatAlias.java
package uth.edu.appchat.Models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "chat_aliases",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_chat_alias_user_scope",
                columnNames = {"user_id","scope_type","scope_id"}
        ),
        indexes = {
                @Index(name="idx_chat_alias_user", columnList = "user_id"),
                @Index(name="idx_chat_alias_scope", columnList = "scope_type,scope_id"),
                @Index(name="idx_chat_alias_updated", columnList = "updated_at")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatAlias {

    public enum ScopeType { PUBLIC, PRIVATE, GROUP }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="user_id", nullable=false, foreignKey=@ForeignKey(name="fk_chat_alias_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name="scope_type", nullable=false, length=16)
    private ScopeType scopeType;

    // Với PUBLIC có thể để null hoặc 0; PRIVATE/GROUP để id phòng
    @Column(name="scope_id")
    private Long scopeId;

    @Column(name="title_alias", length=100)
    private String titleAlias;

    @CreationTimestamp
    @Column(name="created_at", updatable=false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name="updated_at")
    private LocalDateTime updatedAt;
}
