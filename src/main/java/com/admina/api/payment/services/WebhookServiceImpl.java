package com.admina.api.payment.services;

import com.admina.api.config.properties.AppStripeProperties;
import com.admina.api.exceptions.AppExceptions;
import com.admina.api.payment.model.ProcessedWebhook;
import com.admina.api.payment.repository.WebhookRepository;
import com.admina.api.user.enums.PlanType;
import com.admina.api.user.model.User;
import com.admina.api.user.repository.UserRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.checkout.Session;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.StripeObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

        User user = userRepository.findByStripeCustomerIdForUpdate(customerId)
                .orElseThrow(() -> {
                    log.error("No user found for customerId={} event={}",
                            customerId, event.getId());
                    return new AppExceptions.ResourceNotFoundException(
                            "User not found for Stripe customer");
                });

        if (webhookRepository.existsById(event.getId())) {
            log.info("Duplicate webhook ignored eventId={}", event.getId());
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

        webhookRepository.saveAndFlush(
                new ProcessedWebhook(event.getId(), event.getType()));
    }

    private boolean handleCheckoutCompleted(Event event, User user) {
        Session session = deserialize(event, Session.class);
        if (!"paid".equals(session.getPaymentStatus())) {
            return false;
        }

        String planFromStripeMetadata = session.getMetadata() != null ? session.getMetadata().get("plan") : null;
        if (planFromStripeMetadata == null) {
            log.error("Missing plan metadata on session={} event={}", session.getId(), event.getId());
            return false;
        }

        try {
            PlanType plan = PlanType.valueOf(planFromStripeMetadata);

            applyPlan(user, plan);
            log.info("Checkout completed: provisioned user={} plan={}", user.getEmail(), plan);
            return true;

        } catch (IllegalArgumentException e) {
            log.error("Unknown plan value={} event={}", planFromStripeMetadata, event.getId());
            return false;
        }
    }

    private boolean handleInvoicePaid(Event event, User user) {
        Invoice invoice = deserialize(event, Invoice.class);

        if (!"subscription_cycle".equals(invoice.getBillingReason())) {
            log.info("Invoice ignored billingReason={} event={}",
                    invoice.getBillingReason(), event.getId());
            return true;
        }

        String priceId = invoice.getLines().getData().get(0).getPricing().getPriceDetails().getPrice();
        PlanType plan = resolvePlanFromPriceId(priceId);
        if (plan == null) {
            log.error("Unknown priceId={} on invoice={} event={}",
                    priceId, invoice.getId(), event.getId());
            return false;
        }

        applyPlan(user, plan);
        log.info("Invoice paid: renewed limits for user={} plan={}", user.getEmail(), plan);
        return true;
    }

    private boolean handleSubscriptionChange(Event event, User user) {
        Subscription subscription = deserialize(event, Subscription.class);
        String status = subscription.getStatus();

        if ("canceled".equals(status) || "unpaid".equals(status) || "past_due".equals(status)) {
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
            if (user.getPlan() == newPlan) {
                log.info("Subscription update ignored, plan unchanged user={} plan={}",
                        user.getEmail(), newPlan);
                return true;
            }

            applyPlan(user, newPlan);
            log.info("Subscription updated: user={} newPlan={}", user.getEmail(), newPlan);
            return true;
        }

        log.info("Subscription status={} ignored for user={}", status, user.getEmail());
        return true;
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