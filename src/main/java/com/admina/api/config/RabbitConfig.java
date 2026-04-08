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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.*;

@Configuration
public class RabbitConfig {

    // --- GLOBAL DEAD LETTER QUEUE (DLQ) CONSTANTS ---
    public static final String GLOBAL_DLX = "system.dlx";
    public static final String GLOBAL_DLQ = "system.dlq";
    public static final String GLOBAL_DLQ_ROUTING_KEY = "system.dead.letter";

    // --- NOTIFICATIONS CONSTANTS ---
    public static final String NOTIFICATION_EXCHANGE = "notifications.exchange";
    public static final String NOTIFICATION_QUEUE = "notifications.welcome.queue";
    public static final String NOTIFICATION_ROUTING_KEY = "notifications.welcome";

    // --- DOCUMENTS CONSTANTS ---
    public static final String DOC_EXCHANGE = "documents.exchange";
    public static final String DOC_QUEUE = "documents.process.queue";
    public static final String DOC_ROUTING_KEY = "documents.process";

    // --- SUBSCRIPTIONS CONSTANTS ---
    public static final String SUBSCRIPTION_EXCHANGE = "subscriptions.exchange";
    public static final String SUBSCRIPTION_QUEUE = "subscriptions.updated.queue";
    public static final String SUBSCRIPTION_ROUTING_KEY = "subscriptions.updated";

    // ==========================================
    // 1. GLOBAL DEAD LETTER QUEUE SETUP
    // ==========================================
    
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
        return BindingBuilder.bind(globalDlq())
                .to(globalDlx())
                .with(GLOBAL_DLQ_ROUTING_KEY);
    }

    // ==========================================
    // 2. NOTIFICATIONS SETUP
    // ==========================================
    
    @Bean
    public DirectExchange notificationsExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public Queue welcomeQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", GLOBAL_DLX)
                .withArgument("x-dead-letter-routing-key", GLOBAL_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding welcomeBinding() {
        return BindingBuilder.bind(welcomeQueue())
                .to(notificationsExchange())
                .with(NOTIFICATION_ROUTING_KEY);
    }

    // ==========================================
    // 3. DOCUMENTS SETUP
    // ==========================================
    
    @Bean
    public DirectExchange documentsExchange() {
        return new DirectExchange(DOC_EXCHANGE);
    }

    @Bean
    public Queue documentsQueue() {
        return QueueBuilder.durable(DOC_QUEUE)
                .withArgument("x-dead-letter-exchange", GLOBAL_DLX)
                .withArgument("x-dead-letter-routing-key", GLOBAL_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding documentsBinding() {
        return BindingBuilder.bind(documentsQueue())
                .to(documentsExchange())
                .with(DOC_ROUTING_KEY);
    }

    // ==========================================
    // 4. SUBSCRIPTIONS SETUP
    // ==========================================
    
    @Bean
    public DirectExchange subscriptionsExchange() {
        return new DirectExchange(SUBSCRIPTION_EXCHANGE);
    }

    @Bean
    public Queue subscriptionsQueue() {
        return QueueBuilder.durable(SUBSCRIPTION_QUEUE)
                .withArgument("x-dead-letter-exchange", GLOBAL_DLX)
                .withArgument("x-dead-letter-routing-key", GLOBAL_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding subscriptionsBinding() {
        return BindingBuilder.bind(subscriptionsQueue())
                .to(subscriptionsExchange())
                .with(SUBSCRIPTION_ROUTING_KEY);
    }

    // ==========================================
    // 5. INFRASTRUCTURE & FACTORIES
    // ==========================================

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
    public TaskExecutor defaultRabbitTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("rabbit-listener-");
        executor.setVirtualThreads(true);
        return executor;
    }

    @Bean
    public TaskExecutor documentTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("doc-listener-");
        executor.setVirtualThreads(true);
        return executor;
    }

    @Bean
    public TaskExecutor notificationTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("notif-listener-");
        executor.setVirtualThreads(true);
        return executor;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter,
            @Qualifier("defaultRabbitTaskExecutor") TaskExecutor rabbitTaskExecutor) {
        return buildRetryingFactory(connectionFactory, messageConverter, rabbitTaskExecutor, 1, 1);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory notificationListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter,
            @Qualifier("notificationTaskExecutor") TaskExecutor notificationTaskExecutor) {
        return buildRetryingFactory(connectionFactory, messageConverter, notificationTaskExecutor, 2, 5);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory documentListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter,
            @Qualifier("documentTaskExecutor") TaskExecutor documentTaskExecutor) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setTaskExecutor(documentTaskExecutor);
        factory.setConcurrentConsumers(5);
        factory.setMaxConcurrentConsumers(20);
        // Fail-fast logic: no retries, immediately reject to the DLQ
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    private SimpleRabbitListenerContainerFactory buildRetryingFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter,
            TaskExecutor rabbitVirtualTaskExecutor,
            int concurrency,
            int maxConcurrency) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setTaskExecutor(rabbitVirtualTaskExecutor);
        factory.setConcurrentConsumers(concurrency);
        factory.setMaxConcurrentConsumers(maxConcurrency);
        factory.setAdviceChain(
                RetryInterceptorBuilder.stateless()
                        .maxAttempts(3)
                        .backOffPolicy(new JitterExponentialBackOffPolicy(1000, 2.0, 8000, 300))
                        .recoverer(new RejectAndDontRequeueRecoverer()) // Rejects on 3rd fail, routes to DLQ
                        .build());
        return factory;
    }

    private static final class JitterExponentialBackOffPolicy
            implements BackOffPolicy {
        private final long initialInterval;
        private final double multiplier;
        private final long maxInterval;
        private final long maxJitterMs;
        private final Sleeper sleeper = new ThreadWaitSleeper();

        private JitterExponentialBackOffPolicy(long initialInterval, double multiplier, long maxInterval,
                long maxJitterMs) {
            this.initialInterval = initialInterval;
            this.multiplier = multiplier;
            this.maxInterval = maxInterval;
            this.maxJitterMs = maxJitterMs;
        }

        @Override
        public BackOffContext start(RetryContext context) {
            return new Context(initialInterval);
        }

        @Override
        public void backOff(BackOffContext backOffContext)
                throws BackOffInterruptedException {
            Context context = (Context) backOffContext;
            long jitter = (maxJitterMs <= 0) ? 0
                    : java.util.concurrent.ThreadLocalRandom.current().nextLong(maxJitterMs + 1);
            long sleepTime = context.currentInterval + jitter;
            try {
                sleeper.sleep(sleepTime);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new BackOffInterruptedException(
                        "Thread interrupted while backing off", ex);
            }
            long next = Math.min(maxInterval, (long) (context.currentInterval * multiplier));
            context.currentInterval = next;
        }

        private static final class Context implements BackOffContext {
            private long currentInterval;

            private Context(long currentInterval) {
                this.currentInterval = currentInterval;
            }
        }
    }
}