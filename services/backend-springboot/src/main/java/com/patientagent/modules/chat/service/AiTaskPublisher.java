package com.patientagent.modules.chat.service;

import com.patientagent.config.rabbitmq.RabbitMQConfig;
import com.patientagent.modules.chat.dto.AiTaskMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * AI 处理任务消息发布者。
 * <p>
 * 封装了向 {@code chat.task.queue} 发送任务消息的逻辑，
 * 调用此类的服务无需关心 RabbitMQ 的具体实现细节。
 * </p>
 */
@Component
public class AiTaskPublisher {

    private final RabbitTemplate rabbitTemplate;

    public AiTaskPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 将一个 AI 处理任务投入消息队列。
     *
     * @param sessionId 会话编号，用于 Worker 处理完成后关联回复消息到对应会话
     * @param userId    用户 ID
     * @param message   用户发送的消息内容
     */
    public void publish(String sessionId, Long userId, String message) {
        AiTaskMessage taskMessage = new AiTaskMessage();
        taskMessage.setSessionId(sessionId);
        taskMessage.setUserId(userId);
        taskMessage.setMessage(message);
        // 将任务消息发布到 AI 处理任务队列。
        rabbitTemplate.convertAndSend(RabbitMQConfig.CHAT_TASK_QUEUE, taskMessage);
    }
}