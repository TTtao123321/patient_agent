package com.patientagent.modules.chat.service;

import com.patientagent.config.rabbitmq.RabbitMQConfig;
import com.patientagent.modules.chat.dto.AiTaskMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class AiTaskPublisher {

    private final RabbitTemplate rabbitTemplate;

    public AiTaskPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(String sessionId, Long userId, String message) {
        AiTaskMessage taskMessage = new AiTaskMessage();
        taskMessage.setSessionId(sessionId);
        taskMessage.setUserId(userId);
        taskMessage.setMessage(message);
        rabbitTemplate.convertAndSend(RabbitMQConfig.CHAT_TASK_QUEUE, taskMessage);
    }
}