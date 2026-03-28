package com.admina.api.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "notifications.exchange";
    public static final String QUEUE = "notifications.welcome.queue";
    public static final String ROUTING_KEY = "notifications.welcome";
    public static final String DLX = "notifications.dlx";
    public static final String DLQ = "notifications.welcome.dlq";
    public static final String DOC_EXCHANGE = "documents.exchange";
    public static final String DOC_QUEUE = "documents.process.queue";
    public static final String DOC_ROUTING_KEY = "documents.process";

    @Bean
    public DirectExchange notificationsExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public DirectExchange notificationsDlx() {
        return new DirectExchange(DLX);
    }

    @Bean
    public Queue welcomeQueue() {
        return QueueBuilder.durable(QUEUE)
            .withArgument("x-dead-letter-exchange", DLX)
            .withArgument("x-dead-letter-routing-key", ROUTING_KEY + ".dlq")
            .build();
    }

    @Bean
    public Queue welcomeDlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding welcomeBinding() {
        return BindingBuilder.bind(welcomeQueue())
            .to(notificationsExchange())
            .with(ROUTING_KEY);
    }

    @Bean
    public Binding welcomeDlqBinding() {
        return BindingBuilder.bind(welcomeDlq())
            .to(notificationsDlx())
            .with(ROUTING_KEY + ".dlq");
    }

    @Bean
    public DirectExchange documentsExchange() {
        return new DirectExchange(DOC_EXCHANGE);
    }

    @Bean
    public Queue documentsQueue() {
        return QueueBuilder.durable(DOC_QUEUE).build();
    }

    @Bean
    public Binding documentsBinding() {
        return BindingBuilder.bind(documentsQueue())
            .to(documentsExchange())
            .with(DOC_ROUTING_KEY);
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

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
        ConnectionFactory connectionFactory,
        MessageConverter messageConverter
    ) {
        return buildRetryingFactory(connectionFactory, messageConverter, 1);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory notificationListenerContainerFactory(
        ConnectionFactory connectionFactory,
        MessageConverter messageConverter
    ) {
        return buildRetryingFactory(connectionFactory, messageConverter, 1);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory documentListenerContainerFactory(
        ConnectionFactory connectionFactory,
        MessageConverter messageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setConcurrentConsumers(5);
        factory.setMaxConcurrentConsumers(5);
        return factory;
    }

    private SimpleRabbitListenerContainerFactory buildRetryingFactory(
        ConnectionFactory connectionFactory,
        MessageConverter messageConverter,
        int concurrency
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setConcurrentConsumers(concurrency);
        factory.setMaxConcurrentConsumers(concurrency);
        factory.setAdviceChain(
            RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffPolicy(new JitterExponentialBackOffPolicy(1000, 2.0, 8000, 300))
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build()
        );
        return factory;
    }

    private static final class JitterExponentialBackOffPolicy implements org.springframework.retry.backoff.BackOffPolicy {
        private final long initialInterval;
        private final double multiplier;
        private final long maxInterval;
        private final long maxJitterMs;
        private final org.springframework.retry.backoff.Sleeper sleeper =
            new org.springframework.retry.backoff.ThreadWaitSleeper();

        private JitterExponentialBackOffPolicy(long initialInterval, double multiplier, long maxInterval, long maxJitterMs) {
            this.initialInterval = initialInterval;
            this.multiplier = multiplier;
            this.maxInterval = maxInterval;
            this.maxJitterMs = maxJitterMs;
        }

        @Override
        public org.springframework.retry.backoff.BackOffContext start(org.springframework.retry.RetryContext context) {
            return new Context(initialInterval);
        }

        @Override
        public void backOff(org.springframework.retry.backoff.BackOffContext backOffContext) throws org.springframework.retry.backoff.BackOffInterruptedException {
            Context context = (Context) backOffContext;
            long jitter = (maxJitterMs <= 0) ? 0 : java.util.concurrent.ThreadLocalRandom.current().nextLong(maxJitterMs + 1);
            long sleepTime = context.currentInterval + jitter;
            try {
                sleeper.sleep(sleepTime);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new org.springframework.retry.backoff.BackOffInterruptedException("Thread interrupted while backing off", ex);
            }
            long next = Math.min(maxInterval, (long) (context.currentInterval * multiplier));
            context.currentInterval = next;
        }

        private static final class Context implements org.springframework.retry.backoff.BackOffContext {
            private long currentInterval;

            private Context(long currentInterval) {
                this.currentInterval = currentInterval;
            }
        }
    }
}
