// src/main/java/uth/edu/appchat/Repositories/ChatAliasRepository.java
package uth.edu.appchat.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uth.edu.appchat.Models.ChatAlias;
import uth.edu.appchat.Models.ChatAlias.ScopeType;

import java.util.Optional;

@Repository
public interface ChatAliasRepository extends JpaRepository<ChatAlias, Long> {

    Optional<ChatAlias> findByUserIdAndScopeTypeAndScopeId(Long userId, ScopeType scopeType, Long scopeId);

    // public (nếu muốn)
    Optional<ChatAlias> findByUserIdAndScopeType(Long userId, ScopeType scopeType);
}
