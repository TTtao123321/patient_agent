package com.patientagent.config.rabbitmq;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue chatTaskQueue() {
        return new Queue("chat.task.queue", true);
    }
}
