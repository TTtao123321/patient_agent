package com.patientagent.modules.chat.repository;

import com.patientagent.modules.chat.entity.ChatMessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    Page<ChatMessageEntity> findBySessionIdAndIsDeletedOrderBySequenceNoAsc(Long sessionId, Integer isDeleted, Pageable pageable);

    long countBySessionIdAndIsDeleted(Long sessionId, Integer isDeleted);
}
