package com.admina.api.config.rabbit;

import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

@Configuration
public class DocumentRabbitConfig extends RabbitBaseClass {

    public static final String DOC_EXCHANGE = "documents.exchange";
    public static final String DOC_QUEUE = "documents.process.queue";
    public static final String DOC_ROUTING_KEY = "documents.process";

    // 1. Create the Queue/Exchange using the Parent's helper
    @Bean
    public Declarables documentTopology() {
        return buildTopology(DOC_EXCHANGE, DOC_QUEUE, DOC_ROUTING_KEY);
    }

    // 2. Setup Document specific threading
    @Bean
    public TaskExecutor documentTaskExecutor() {
        return buildVirtualTaskExecutor("doc-listener-");
    }

    // 3. Setup Document specific fail-fast worker logic
    @Bean
    public SimpleRabbitListenerContainerFactory documentListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        return buildFailFastFactory(connectionFactory, messageConverter, documentTaskExecutor(), 5, 20);
    }
}