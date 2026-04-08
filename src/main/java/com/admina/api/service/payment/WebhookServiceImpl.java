package com.admina.api.service.payment;

import com.admina.api.config.properties.AppPlanProperties;
import com.admina.api.config.properties.AppStripeProperties;
import com.admina.api.enums.PlanType;
import com.admina.api.events.subscription.SubscriptionUpdatedEvent;
import com.admina.api.exceptions.AppExceptions;
import com.admina.api.model.subscription.UserStripeSubscription;
import com.admina.api.model.user.User;
import com.admina.api.repository.SubscriptionRepository;
import com.admina.api.repository.UserRepository;
import com.admina.api.repository.WebhookEventRepository;
import com.admina.api.model.webhook.WebhookEvent;
import com.admina.api.pub.WebhookPublisher;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.checkout.Session;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.SubscriptionItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookServiceImpl implements WebhookService {

    private final AppStripeProperties stripeProperties;
    private final AppPlanProperties planProperties;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final WebhookPublisher webhookPublisher;

    @Override
    public void handleEvent(byte[] payload, String sigHeader) {
        // 1. Verify Stripe signature
        Event event;
        try {
            event = Webhook.constructEvent(
                    new String(payload),
                    sigHeader,
                    stripeProperties.webhookSecret());
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe webhook signature: {}", e.getMessage());
            throw new AppExceptions.BadRequestException("Invalid webhook signature");
        }

        // 2. Deduplicate — skip if already processed
        if (webhookEventRepository.existsByEventId(event.getId())) {
            log.info("Skipping duplicate webhook event={}", event.getId());
            return;
        }

        // 3. Route event type
        switch (event.getType()) {
            case "checkout.session.completed" -> handleCheckoutSessionCompleted(event);
            default -> log.info("Unhandled Stripe event type={}", event.getType());
        }
    }

    @Transactional
    protected void handleCheckoutSessionCompleted(Event event) {
        // 4. Deserialize session
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (!deserializer.getObject().isPresent()) {
            log.error("Failed to deserialize checkout session event={}", event.getId());
            throw new AppExceptions.InternalServerErrorException("Webhook deserialization failed");
        }
        Session session = (Session) deserializer.getObject().get();

        // 5. Find user by Stripe customer ID
        String customerId = session.getCustomer();
        User user = userRepository.findByStripeCustomerId(customerId)
                .orElseThrow(() -> {
                    log.error("No user found for stripeCustomerId={}", customerId);
                    return new AppExceptions.ResourceNotFoundException("User not found for customer: " + customerId);
                });

        // 6. Retrieve subscription from Stripe
        Subscription stripeSub;
        try {
            stripeSub = Subscription.retrieve(session.getSubscription());
        } catch (Exception e) {
            log.error("Failed to retrieve Stripe subscription={} error={}", session.getSubscription(), e.getMessage());
            throw new AppExceptions.InternalServerErrorException("Failed to retrieve subscription");
        }

        SubscriptionItem item = getPeriodSourceItem(stripeSub);

        // 7. Resolve plan from Stripe price ID
        String priceId = item.getPrice().getId();
        PlanType plan = resolvePlan(priceId);

        // 8. Sync user plan
        user.setPlan(plan);
        user.setPlanLimitMax(planProperties.getMaxForPlan(plan));
        user.setPlanLimitCurrent(0);
        user.setStripeCustomerId(customerId);

        // 9. Upsert subscription record
        UserStripeSubscription subscription = subscriptionRepository
                .findByStripeSubscriptionId(stripeSub.getId())
                .orElse(new UserStripeSubscription());

        subscription.setUser(user);
        subscription.setStripeSubscriptionId(stripeSub.getId());
        subscription.setStripeCustomerId(customerId);
        subscription.setStatus(stripeSub.getStatus());
        subscription.setPlan(plan);
        subscription.setCurrentPeriodStart(Instant.ofEpochSecond(item.getCurrentPeriodStart()));
        subscription.setCurrentPeriodEnd(Instant.ofEpochSecond(item.getCurrentPeriodEnd()));
        subscriptionRepository.save(subscription);

        // 10. Mark webhook event as processed
        webhookEventRepository.save(new WebhookEvent(event.getId(), event.getType()));

        // 11. Publish RabbitMQ event
        webhookPublisher.publish(
                new SubscriptionUpdatedEvent(
                        user.getId(),
                        user.getEmail(),
                        plan,
                        stripeSub.getId()));
    }

    private PlanType resolvePlan(String priceId) {
        return stripeProperties.priceIds().entrySet().stream()
                .filter(entry -> entry.getValue().equals(priceId))
                .map(entry -> PlanType.valueOf(entry.getKey().toUpperCase()))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("Unknown Stripe priceId={}", priceId);
                    return new AppExceptions.BadRequestException("Unknown price ID: " + priceId);
                });
    }

    private SubscriptionItem getPeriodSourceItem(Subscription stripeSub) {
        return Optional.ofNullable(stripeSub.getItems())
                .map(items -> items.getData())
                .filter(items -> !items.isEmpty())
                .map(items -> items.get(0))
                .orElseThrow(() -> {
                    log.error("Stripe subscription has no items subscriptionId={}", stripeSub.getId());
                    return new AppExceptions.InternalServerErrorException("Subscription has no items");
                });
    }
}
