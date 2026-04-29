package com.admina.api.config.rabbit;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitCoreConfig {

    public static final String GLOBAL_DLX = "system.dlx";
    public static final String GLOBAL_DLQ = "system.dlq";
    public static final String GLOBAL_DLQ_ROUTING_KEY = "system.dead.letter";

    @Bean
    public DirectExchange globalDlx() {
        return new DirectExchange(GLOBAL_DLX);
    }

    @Bean
    public Queue globalDlq() {
        return QueueBuilder.durable(GLOBAL_DLQ).build();
    }

    @Bean
    public Binding globalDlqBinding() {
        return BindingBuilder.bind(globalDlq()).to(globalDlx()).with(GLOBAL_DLQ_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}