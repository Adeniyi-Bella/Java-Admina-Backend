package com.admina.api.config.rabbit;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.retry.backoff.BackOffContext;
import org.springframework.retry.backoff.BackOffInterruptedException;
import org.springframework.retry.backoff.ThreadWaitSleeper;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.*;


public abstract class RabbitBaseClass {

    /**
     * Builds standard Topology (Exchange, Queue, Binding) and automatically routes
     * failures to the Global DLQ.
     */
    protected Declarables buildTopology(String exchangeName, String queueName, String routingKey) {
        DirectExchange exchange = new DirectExchange(exchangeName);
        Queue queue = QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", RabbitCoreConfig.GLOBAL_DLX)
                .withArgument("x-dead-letter-routing-key", RabbitCoreConfig.GLOBAL_DLQ_ROUTING_KEY)
                .build();
        Binding binding = BindingBuilder.bind(queue).to(exchange).with(routingKey);

        return new Declarables(exchange, queue, binding);
    }

    /**
     * Helper to create a TaskExecutor with Virtual Threads for high throughput.
     */
    protected TaskExecutor buildVirtualTaskExecutor(String threadNamePrefix) {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor(threadNamePrefix);
        executor.setVirtualThreads(true);
        return executor;
    }

    /**
     * Factory for standard tasks (Retries 3 times with Jitter Backoff before
     * sending to DLQ)
     */
    protected SimpleRabbitListenerContainerFactory buildRetryingFactory(
            ConnectionFactory connectionFactory, MessageConverter messageConverter,
            TaskExecutor executor, int concurrency, int maxConcurrency) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setTaskExecutor(executor);
        factory.setConcurrentConsumers(concurrency);
        factory.setMaxConcurrentConsumers(maxConcurrency);
        factory.setAdviceChain(
                RetryInterceptorBuilder.stateless()
                        .maxAttempts(3)
                        .backOffPolicy(new JitterExponentialBackOffPolicy(1000, 2.0, 8000, 300))
                        .recoverer(new RejectAndDontRequeueRecoverer())
                        .build());
        return factory;
    }

    /**
     * Factory for fail-fast tasks (No retries, immediately rejects and sends to
     * DLQ)
     */
    protected SimpleRabbitListenerContainerFactory buildFailFastFactory(
            ConnectionFactory connectionFactory, MessageConverter messageConverter,
            TaskExecutor executor, int concurrency, int maxConcurrency) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setTaskExecutor(executor);
        factory.setConcurrentConsumers(concurrency);
        factory.setMaxConcurrentConsumers(maxConcurrency);
        factory.setDefaultRequeueRejected(false); // Fail-fast rule
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