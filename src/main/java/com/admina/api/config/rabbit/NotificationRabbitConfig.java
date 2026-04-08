package com.admina.api.config.rabbit;

import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

@Configuration
public class NotificationRabbitConfig extends RabbitBaseClass {

    public static final String NOTIFICATION_EXCHANGE = "notifications.exchange";
    public static final String NOTIFICATION_QUEUE = "notifications.welcome.queue";
    public static final String NOTIFICATION_ROUTING_KEY = "notifications.welcome";

    @Bean
    public Declarables notificationTopology() {
        return buildTopology(NOTIFICATION_EXCHANGE, NOTIFICATION_QUEUE, NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public TaskExecutor notificationTaskExecutor() {
        return buildVirtualTaskExecutor("notif-listener-");
    }

    @Bean
    public SimpleRabbitListenerContainerFactory notificationListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        // Notifications use retries!
        return buildRetryingFactory(connectionFactory, messageConverter, notificationTaskExecutor(), 2, 5);
    }
}