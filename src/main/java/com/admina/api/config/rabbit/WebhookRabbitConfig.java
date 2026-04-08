package com.admina.api.config.rabbit;

import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

@Configuration
public class WebhookRabbitConfig extends RabbitBaseClass {

    public static final String SUBSCRIPTION_EXCHANGE = "subscriptions.exchange";
    public static final String SUBSCRIPTION_QUEUE = "subscriptions.updated.queue";
    public static final String SUBSCRIPTION_ROUTING_KEY = "subscriptions.updated";

    // 1. Create the Queue/Exchange using the Parent's helper (automatically attaches the DLQ)
    @Bean
    public Declarables subscriptionTopology() {
        return buildTopology(SUBSCRIPTION_EXCHANGE, SUBSCRIPTION_QUEUE, SUBSCRIPTION_ROUTING_KEY);
    }

    // 2. Setup Subscription specific threading
    @Bean
    public TaskExecutor subscriptionTaskExecutor() {
        return buildVirtualTaskExecutor("sub-listener-");
    }

    // 3. Setup Subscription specific worker logic
    @Bean
    public SimpleRabbitListenerContainerFactory subscriptionListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        
        // Using retries, with exactly 1 concurrent consumer (matching your original default factory)
        return buildRetryingFactory(connectionFactory, messageConverter, subscriptionTaskExecutor(), 1, 1);
    }
}