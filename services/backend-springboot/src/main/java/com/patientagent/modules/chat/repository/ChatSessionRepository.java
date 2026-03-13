package com.patientagent.modules.chat.repository;

import com.patientagent.modules.chat.entity.ChatSessionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 聊天会话数据访问层。
 */
public interface ChatSessionRepository extends JpaRepository<ChatSessionEntity, Long> {

    /**
     * 根据会话编号查询未删除的会话。
     */
    Optional<ChatSessionEntity> findBySessionNoAndIsDeleted(String sessionNo, Integer isDeleted);

    /**
     * 查询指定用户的未删除会话，按最新消息时间倒序分页。
     */
    Page<ChatSessionEntity> findByUserIdAndIsDeletedOrderByLastMessageAtDesc(Long userId, Integer isDeleted, Pageable pageable);
}
