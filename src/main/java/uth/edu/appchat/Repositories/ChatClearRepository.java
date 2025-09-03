// src/main/java/uth/edu/appchat/Repositories/ChatClearRepository.java
package uth.edu.appchat.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import uth.edu.appchat.Models.ChatClear;

public interface ChatClearRepository extends JpaRepository<ChatClear, ChatClear.ChatClearId> {
}
