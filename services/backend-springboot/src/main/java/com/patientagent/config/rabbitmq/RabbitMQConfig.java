package com.patientagent.config.rabbitmq;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置类，声明 AI 任务队列和消息模式相关 Bean。
 * <p>
 * 消息使用 Jackson JSON 序列化，支持 Java 对象与 JSON 的自动转换。
 * 消费者使用 AUTO_ACK 模式，任务处理完毕后自动确认。
 * </p>
 */
@Configuration
public class RabbitMQConfig {

    /** AI 处理任务队列名。 */
    public static final String CHAT_TASK_QUEUE = "chat.task.queue";

    /**
     * 声明持久化的 AI 任务队列（{@code durable=true}，即使 broker 重启消息不丢失）。
     */
    @Bean
    public Queue chatTaskQueue() {
        return new Queue(CHAT_TASK_QUEUE, true);
    }

    /**
     * 基于 Jackson 的 AMQP 消息转换器，自动序列化 / 反序列化 Java 对象。
     */
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 配置 {@link RabbitTemplate}，用于向队列发送消息。
     */
    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter messageConverter
    ) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

    /**
     * 配置监听器容器工厂，设置消公序列化方式和 ACK 模式。
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter messageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        // AUTO_ACK 模式：方法正常返回后自动应答，抛出异常则拒绝确认。
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        return factory;
    }
}
