package com.ssafy.tax7i.ai.repository;

import com.ssafy.tax7i.ai.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    Optional<ChatSession> findBySessionId(String sessionId);

    Optional<ChatSession> findBySessionIdAndUserId(String sessionId, Long userId);
}
