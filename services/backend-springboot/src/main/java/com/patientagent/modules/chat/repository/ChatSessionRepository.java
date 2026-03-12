package com.patientagent.modules.chat.repository;

import com.patientagent.modules.chat.entity.ChatSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSessionEntity, Long> {

    Optional<ChatSessionEntity> findBySessionNoAndIsDeleted(String sessionNo, Integer isDeleted);
}
