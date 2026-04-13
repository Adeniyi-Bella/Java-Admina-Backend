package com.admina.api.service.payment;

import com.admina.api.config.properties.AppStripeProperties;
import com.admina.api.enums.PlanType;
import com.admina.api.exceptions.AppExceptions;
import com.admina.api.model.user.User;
import com.admina.api.repository.UserRepository;
import com.admina.api.repository.WebhookRepository;
import com.admina.api.model.webhook.ProcessedWebhook;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.checkout.Session;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.StripeObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookServiceImpl implements WebhookService {

    private final AppStripeProperties stripeProperties;
    private final UserRepository userRepository;
    private final WebhookRepository webhookRepository;

    @Transactional
    @Override
    public void handleStripeWebhook(byte[] payload, String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(
                    new String(payload, StandardCharsets.UTF_8),
                    sigHeader,
                    stripeProperties.webhookSecret());
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe webhook signature: {}", e.getMessage());
            throw new AppExceptions.UnauthorizedException("Invalid webhook signature");
        }

        String customerId = extractCustomerId(event);
        if (customerId == null) {
            log.warn("Could not extract customerId from event={} type={}",
                    event.getId(), event.getType());
            return;
        }

        User user = userRepository.findByStripeCustomerIdForUpdate(customerId).orElse(null);
        if (user == null) {
            log.error("No user found for customerId={} event={}",
                    customerId, event.getId());
            return;
        }

        boolean handled = switch (event.getType()) {
            case "checkout.session.completed" -> handleCheckoutCompleted(event, user);
            case "invoice.paid" -> handleInvoicePaid(event, user);
            case "customer.subscription.deleted",
                    "customer.subscription.updated" ->
                handleSubscriptionChange(event, user);
            default -> {
                log.info("Unhandled Stripe event type={}", event.getType());
                yield false;
            }
        };

        if (!handled)
            return;

        try {
            webhookRepository.saveAndFlush(
                    new ProcessedWebhook(event.getId(), event.getType()));
        } catch (DataIntegrityViolationException e) {
            log.info("Ignored duplicate Stripe webhook: {}", event.getId());
            return;
        }

        userRepository.save(user);
    }

    private boolean handleCheckoutCompleted(Event event, User user) {
        Session session = deserialize(event, Session.class);
        if ("paid".equals(session.getPaymentStatus())) {
            PlanType plan = PlanType.valueOf(session.getMetadata().get("plan"));
            applyPlan(user, plan);
            log.info("Checkout completed: Provisioned user={} plan={}", user.getEmail(), plan);
            return true;
        }
        return false;
    }

    private boolean handleInvoicePaid(Event event, User user) {
        Invoice invoice = deserialize(event, Invoice.class);
        if ("subscription_cycle".equals(invoice.getBillingReason())) {
            applyPlan(user, user.getPlan());
            log.info("Invoice paid: Renewed limits for user={} plan={}", user.getEmail(), user.getPlan());
            return true;
        }
        return false;
    }

    private boolean handleSubscriptionChange(Event event, User user) {
        Subscription subscription = deserialize(event, Subscription.class);
        String status = subscription.getStatus();

        if ("canceled".equals(status) || "unpaid".equals(status)) {
            applyPlan(user, PlanType.FREE);
            log.info("Subscription {}: Downgraded user={} to FREE", status, user.getEmail());
            return true;
        }

        if ("active".equals(status)) {
            String newPriceId = subscription.getItems().getData().get(0).getPrice().getId();
            PlanType newPlan = resolvePlanFromPriceId(newPriceId);
            if (newPlan == null) {
                log.warn("Unknown priceId={} event={}", newPriceId, event.getId());
                return false;
            }
            if (user.getPlan() == newPlan)
                return false;
            applyPlan(user, newPlan);
            log.info("Subscription updated: user={} plan={}", user.getEmail(), newPlan);
            return true;
        }

        log.info("Subscription status={} ignored for user={}", status, user.getEmail());
        return false;
    }

    private void applyPlan(User user, PlanType plan) {
        user.setPlan(plan);
        user.setDocumentsUsed(plan.getMaxDocuments());
    }

    private <T extends StripeObject> T deserialize(Event event, Class<T> type) {
        return type.cast(event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> {
                    log.error("Failed to deserialize event={} type={} expected={}",
                            event.getId(), event.getType(), type.getSimpleName());
                    return new AppExceptions.InternalServerErrorException(
                            "Failed to deserialize webhook payload");
                }));
    }

    private PlanType resolvePlanFromPriceId(String priceId) {
        return Arrays.stream(PlanType.values())
                .filter(plan -> priceId.equals(stripeProperties.getPriceIdForPlan(plan.name())))
                .findFirst()
                .orElse(null);
    }

    private String extractCustomerId(Event event) {
        StripeObject stripeObject = event.getDataObjectDeserializer()
                .getObject().orElse(null);
        if (stripeObject instanceof Session session)
            return session.getCustomer();
        if (stripeObject instanceof Subscription subscription)
            return subscription.getCustomer();
        if (stripeObject instanceof Invoice invoice)
            return invoice.getCustomer();
        return null;
    }
}