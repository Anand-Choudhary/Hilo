package com.chatapp.chat_backend.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig
{
    public static final String CHAT_EXCHANGE = "chat.exchange";
    public static final String MESSAGE_QUEUE = "chat.message.queue";
    public static final String NOTIFICATION_QUEUE = "chat.notification.queue";
    public static final String MESSAGE_ROUTING_KEY = "chat.message";
    public static final String NOTIFICATION_ROUTING_KEY = "chat.notification";

    @Bean
    public DirectExchange chatExchange() {
        return new DirectExchange(CHAT_EXCHANGE);
    }

    @Bean
    public Queue messageQueue() {
        return QueueBuilder.durable(MESSAGE_QUEUE)
                .withArgument("x-message-ttl", 86400000) // 24 hours
                .build();
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .withArgument("x-message-ttl", 86400000)
                .build();
    }

    @Bean
    public Binding messageBinding(Queue messageQueue, DirectExchange chatExchange) {
        return BindingBuilder.bind(messageQueue)
                .to(chatExchange)
                .with(MESSAGE_ROUTING_KEY);
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, DirectExchange chatExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(chatExchange)
                .with(NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}
