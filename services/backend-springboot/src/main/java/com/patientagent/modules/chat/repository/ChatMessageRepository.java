package com.patientagent.modules.chat.repository;

import com.patientagent.modules.chat.entity.ChatMessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 聊天消息数据访问层。
 * <p>继承 {@link JpaRepository}，通过方法命后规则自动实现常用查询。</p>
 */
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    /**
     * 分页查询指定会话中未删除的消息，按序列号升序排列。
     *
     * @param sessionId 会话主键 ID
     * @param isDeleted 是否删除标志：0 = 未删除
     * @param pageable  分页参数
     */
    Page<ChatMessageEntity> findBySessionIdAndIsDeletedOrderBySequenceNoAsc(Long sessionId, Integer isDeleted, Pageable pageable);

    /**
     * 统计指定会话中未删除的消息总条数，用于计算下一条消息的序列号。
     */
    long countBySessionIdAndIsDeleted(Long sessionId, Integer isDeleted);
}
