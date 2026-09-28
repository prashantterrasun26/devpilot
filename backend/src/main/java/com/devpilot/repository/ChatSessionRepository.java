package com.devpilot.repository;

import com.devpilot.entity.ChatMessage;
import com.devpilot.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    List<ChatSession> findByUserIdAndRepositoryIdOrderByCreatedAtDesc(Long userId, Long repositoryId);
    Optional<ChatSession> findByIdAndUserId(Long id, Long userId);
}
