package com.admina.api.config.rabbit;

import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

@Configuration
public class ChatRabbitConfig extends RabbitBaseClass {

    public static final String CHAT_EXCHANGE = "chat.exchange";
    public static final String CHAT_QUEUE = "chat.process.queue";
    public static final String CHAT_ROUTING_KEY = "chat.process";

    @Bean
    public Declarables chatTopology() {
        return buildTopology(CHAT_EXCHANGE, CHAT_QUEUE, CHAT_ROUTING_KEY);
    }

    @Bean
    public TaskExecutor chatTaskExecutor() {
        return buildVirtualTaskExecutor("chat-listener-");
    }

    @Bean
    public SimpleRabbitListenerContainerFactory chatListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        return buildFailFastFactory(connectionFactory, messageConverter, chatTaskExecutor(), 5, 20);
    }
}
