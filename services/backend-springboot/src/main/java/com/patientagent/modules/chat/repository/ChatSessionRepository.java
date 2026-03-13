package com.patientagent.modules.chat.repository;

import com.patientagent.modules.chat.entity.ChatSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 聊天会话数据访问层。
 */
public interface ChatSessionRepository extends JpaRepository<ChatSessionEntity, Long> {

    /**
     * 根据会话编号查询未删除的会话。
     *
     * @param sessionNo 会话编号（全局唯一）
     * @param isDeleted 是否删除标志：0 = 未删除
     */
    Optional<ChatSessionEntity> findBySessionNoAndIsDeleted(String sessionNo, Integer isDeleted);
}
