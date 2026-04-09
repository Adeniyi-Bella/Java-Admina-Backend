package com.admina.api.config.rabbit;

import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

@Configuration
public class UserDeleteRabbitConfig extends RabbitBaseClass {

    public static final String USER_DELETE_EXCHANGE = "users.delete.exchange";
    public static final String USER_DELETE_QUEUE = "users.delete.entra.queue";
    public static final String USER_DELETE_ROUTING_KEY = "users.delete.entra";

    @Bean
    public Declarables userDeleteTopology() {
        return buildTopology(USER_DELETE_EXCHANGE, USER_DELETE_QUEUE, USER_DELETE_ROUTING_KEY);
    }

    @Bean
    public TaskExecutor userDeleteTaskExecutor() {
        return buildVirtualTaskExecutor("user-delete-listener-");
    }

    @Bean
    public SimpleRabbitListenerContainerFactory userDeleteListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        return buildRetryingFactory(connectionFactory, messageConverter, userDeleteTaskExecutor(), 2, 5);
    }
}
