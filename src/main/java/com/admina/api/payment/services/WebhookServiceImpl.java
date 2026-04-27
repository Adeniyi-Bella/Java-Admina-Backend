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
import com.stripe.net.ApiResource;
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

/**
 * Service implementation responsible for receiving and processing incoming
 * Stripe webhook events.
 *
 * This class is the central handler for all billing-related lifecycle events
 * sent by Stripe.
 * It verifies the authenticity of each webhook, routes the event to the
 * appropriate handler
 * based on its type, and updates the corresponding user's subscription plan
 * accordingly.
 *
 * Idempotency is enforced by persisting the Stripe event ID to the database
 * after successful
 * processing. Any duplicate event delivery (which Stripe may send on retries)
 * is detected
 * and silently ignored.
 *
 * Supported Stripe event types:
 * - checkout.session.completed : Activates a plan after a successful checkout
 * payment.
 * - invoice.paid : Renews a plan's usage limits on a subscription billing
 * cycle.
 * - customer.subscription.updated : Handles plan changes such as upgrades or
 * downgrades.
 * - customer.subscription.deleted : Downgrades the user to the FREE plan on
 * cancellation.
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookServiceImpl implements WebhookService {

    private final AppStripeProperties stripeProperties;
    private final UserRepository userRepository;
    private final WebhookRepository webhookRepository;

    /**
     * Entry point for all incoming Stripe webhook HTTP requests.
     *
     * 1. Verify the Stripe HMAC signature using the raw request body and the
     * "Stripe-Signature" header to confirm the request genuinely came from Stripe.
     * 2. Extract the Stripe Customer ID from the event payload to look up the
     * associated user.
     * 3. Check whether this event ID has already been processed (idempotency
     * guard).
     * 4. Dispatch to the appropriate private handler based on the event type.
     * 5. Persist the event ID to the database so future duplicate deliveries are
     * skipped.
     *
     * The method is annotated with @Transactional so that user plan updates and
     * webhook
     * persistence happen atomically. If any step throws, the entire transaction
     * rolls back
     * and Stripe will retry the delivery.
     *
     * @param payload   The raw UTF-8 bytes of the HTTP request body. Must be the
     *                  unmodified
     *                  original body — any re-serialization will break signature
     *                  verification.
     * @param sigHeader The value of the "Stripe-Signature" HTTP header sent by
     *                  Stripe.
     * @throws AppExceptions.UnauthorizedException     if the signature does not
     *                                                 match.
     * @throws AppExceptions.ResourceNotFoundException if no user is found for the
     *                                                 customer ID.
     */
    @Transactional
    @Override
    public void handleStripeWebhook(byte[] payload, String sigHeader) {
        Event event;
        try {
            // Construct and verify the event. Stripe checks the HMAC-SHA256 signature
            // against the webhook secret configured in AppStripeProperties.
            event = Webhook.constructEvent(
                    new String(payload, StandardCharsets.UTF_8),
                    sigHeader,
                    stripeProperties.webhookSecret());
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe webhook signature: {}", e.getMessage());
            throw new AppExceptions.UnauthorizedException("Invalid webhook signature");
        }
        // Every supported event type carries a customer ID. If it cannot be extracted,
        // the event is malformed or belongs to an unsupported object type — skip it.
        String customerId = extractCustomerId(event);
        if (customerId == null) {
            log.warn("Could not extract customerId from event={} type={}",
                    event.getId(), event.getType());
            return;
        }

        // Use a SELECT FOR UPDATE query to lock the user row for the duration of
        // this transaction, preventing concurrent webhook deliveries from racing
        // each other and corrupting the plan state.
        User user = userRepository.findByStripeCustomerIdForUpdate(customerId)
                .orElseThrow(() -> {
                    log.error("No user found for customerId={} event={}",
                            customerId, event.getId());
                    return new AppExceptions.ResourceNotFoundException(
                            "User not found for Stripe customer");
                });

        // Idempotency check: if this event ID was already processed (e.g. a Stripe
        // retry after a prior successful delivery), ignore it to avoid double-applying
        // plan changes.
        if (webhookRepository.existsById(event.getId())) {
            log.warn("Duplicate webhook ignored eventId={}", event.getId());
            return;
        }

        // Dispatch to the correct handler. Each handler returns true if it made a
        // meaningful change worth recording, or false if the event should be skipped
        // (e.g. unpaid checkout, unrecognised plan, or a no-op status transition).
        boolean handled = switch (event.getType()) {
            case "checkout.session.completed" -> handleCheckoutCompleted(event, user);
            case "invoice.paid" -> handleInvoicePaid(event, user);
            case "customer.subscription.deleted",
                    "customer.subscription.updated" ->
                handleSubscriptionChange(event, user);
            case "invoice.payment_succeeded" -> {
                log.debug("Ignored invoice.payment_succeeded, handled via invoice.paid");
                yield false;
            }
            default -> {
                log.info("Unhandled Stripe event type={}", event.getType());
                yield false;
            }
        };

        // Only persist the event ID when the handler confirms a meaningful action was
        // taken. Unhandled or intentionally skipped events are not recorded so they
        // can be reprocessed later if needed.
        if (!handled)
            return;

        webhookRepository.saveAndFlush(
                new ProcessedWebhook(event.getId(), event.getType()));
    }

    /**
     * Handles the "checkout.session.completed" event.
     *
     * This event fires when a customer completes a Stripe Checkout flow. It is used
     * for the initial purchase of a paid plan. The target plan is read from the
     * session's metadata under the key "plan", which must have been set when
     * creating
     * the Checkout Session on the backend (e.g. metadata: { plan: "PRO" }).
     *
     * The event is skipped (returns false) if:
     * - The payment status is not "paid" (e.g. free trials or pending payments).
     * - The "plan" metadata key is missing from the session.
     * - The "plan" value does not match any known PlanType enum constant.
     *
     * @param event The verified Stripe event.
     * @param user  The user associated with the Stripe customer in the event.
     * @return true if the plan was successfully applied; false otherwise.
     */
    private boolean handleCheckoutCompleted(Event event, User user) {
        Session session = deserialize(event, Session.class);

        // Guard: only process sessions that have actually been paid.
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

            if (session.getSubscription() != null) {
                user.setStripeSubscriptionId(session.getSubscription());
            }

            log.info("Checkout completed: provisioned user={} plan={}", user.getEmail(), plan);
            return true;

        } catch (IllegalArgumentException e) {
            log.error("Unknown plan value={} event={}", planFromStripeMetadata, event.getId());
            return false;
        }
    }

    /**
     * Handles the "invoice.paid" event.
     *
     * This event fires every time Stripe successfully collects payment for an
     * invoice.
     * This handler only acts on invoices with billing reason "subscription_cycle",
     * which
     * represents a recurring renewal charge. Other billing reasons (e.g.
     * "subscription_create"
     * for the first charge, or "manual" for one-off invoices) are intentionally
     * ignored here
     * because plan activation for first-time purchases is handled by
     * handleCheckoutCompleted.
     *
     * On a renewal, the handler re-applies the plan to reset usage counters (e.g.
     * document
     * limits) for the new billing period. The plan is resolved by matching the
     * invoice line
     * item's price ID against the price IDs configured in AppStripeProperties.
     *
     * @param event The verified Stripe event.
     * @param user  The user associated with the Stripe customer in the event.
     * @return true if the renewal was processed or intentionally skipped; false if
     *         the
     *         price ID could not be resolved to a known plan.
     */
    private boolean handleInvoicePaid(Event event, User user) {
        Invoice invoice = deserialize(event, Invoice.class);

        // Only act on initial payment (subscription_create) and renewals
        // (subscription_cycle).
        // Everything else (manual, subscription_update, etc.) is ignored.
        if (!"subscription_cycle".equals(invoice.getBillingReason())
                && !"subscription_create".equals(invoice.getBillingReason())) {
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

    /**
     * Handles "customer.subscription.updated" and "customer.subscription.deleted"
     * events.
     *
     * This method covers two distinct lifecycle transitions:
     *
     * Subscription ended / payment failure:
     * If the subscription status is "canceled", "unpaid", or "past_due", the user
     * is
     * immediately downgraded to the FREE plan. This covers voluntary cancellations
     * as
     * well as failed payment recovery scenarios.
     *
     * Subscription updated (active):
     * If the subscription remains active but its price has changed (e.g. the
     * customer
     * switched from a monthly BASIC plan to an annual PRO plan), the new plan is
     * resolved
     * from the updated price ID and applied. If the plan has not actually changed,
     * the
     * event is acknowledged but no update is performed to avoid unnecessary writes.
     *
     * Any other subscription status (e.g. "trialing", "incomplete") is logged and
     * acknowledged
     * without modification to the user's plan.
     *
     * @param event The verified Stripe event.
     * @param user  The user associated with the Stripe customer in the event.
     * @return true in all cases except when the price ID cannot be resolved to a
     *         known plan.
     */
    private boolean handleSubscriptionChange(Event event, User user) {
        Subscription subscription = deserialize(event, Subscription.class);
        String status = subscription.getStatus();

        // Downgrade to FREE for any terminal or delinquent subscription state.
        if ("canceled".equals(status) || "unpaid".equals(status) || "past_due".equals(status)) {
            applyPlan(user, PlanType.FREE);
            log.info("Subscription {}: Downgraded user={} to FREE", status, user.getEmail());
            return true;
        }

        if ("active".equals(status)) {
            // Resolve the new plan from the price ID on the first subscription item.
            // Standard subscriptions only ever have one item.
            String newPriceId = subscription.getItems().getData().get(0).getPrice().getId();
            PlanType newPlan = resolvePlanFromPriceId(newPriceId);
            if (newPlan == null) {
                log.warn("Unknown priceId={} event={}", newPriceId, event.getId());
                return false;
            }
            // Skip the update if the resolved plan matches what the user already has.
            // This avoids resetting usage counters mid-cycle on a no-op update event.
            if (user.getPlan() == newPlan) {
                log.info("Subscription update ignored, plan unchanged user={} plan={}",
                        user.getEmail(), newPlan);
                return true;
            }

            applyPlan(user, newPlan);
            // Other statuses (e.g. "trialing", "incomplete", "incomplete_expired") are
            // acknowledged without modifying the user's plan.
            log.info("Subscription updated: user={} newPlan={}", user.getEmail(), newPlan);
            return true;
        }

        log.info("Subscription status={} ignored for user={}", status, user.getEmail());
        return true;
    }

    /**
     * Applies a plan to a user by updating both their plan type and resetting their
     * document usage allowance to the maximum permitted by the new plan.
     *
     * This is the single write point for plan state changes across all event
     * handlers.
     * Because the parent transaction is managed by handleStripeWebhook, the changes
     * made here are committed atomically along with the ProcessedWebhook record.
     *
     * @param user The user whose plan should be updated.
     * @param plan The new plan to assign.
     */
    private void applyPlan(User user, PlanType plan) {
        user.setPlan(plan);
        user.setDocumentsUsed(plan.getMaxDocuments());
    }

    /**
     * Deserializes the raw JSON payload embedded in a Stripe event into a typed
     * Stripe model.
     *
     * Stripe's Java SDK may deserialize event data objects using an API version
     * that differs
     * from the one the object was created with, potentially losing fields. To avoid
     * this,
     * the raw JSON string is retrieved directly from the event's
     * DataObjectDeserializer and
     * parsed manually using Stripe's own GSON instance, which is guaranteed to
     * match the
     * Stripe model class structure.
     *
     * @param event The verified Stripe event containing the raw JSON payload.
     * @param type  The target class to deserialize into (e.g. Session.class,
     *              Invoice.class).
     * @param <T>   A type that extends StripeObject.
     * @return The deserialized Stripe object.
     * @throws AppExceptions.InternalServerErrorException if the raw JSON is absent
     *                                                    or if deserialization
     *                                                    fails for any reason.
     */
    private <T extends StripeObject> T deserialize(Event event, Class<T> type) {
        String rawJson = event.getDataObjectDeserializer().getRawJson();
        if (rawJson == null) {
            log.error("No raw JSON in event={} type={}", event.getId(), event.getType());
            throw new AppExceptions.InternalServerErrorException("Failed to deserialize webhook payload");
        }
        try {
            return ApiResource.GSON.fromJson(rawJson, type);
        } catch (Exception ex) {
            log.error("Failed to deserialize event={} type={} expected={}",
                    event.getId(), event.getType(), type.getSimpleName());
            throw new AppExceptions.InternalServerErrorException("Failed to deserialize webhook payload");
        }
    }

    /**
     * Resolves a Stripe Price ID to an internal PlanType by comparing it against
     * the
     * price IDs configured in AppStripeProperties.
     *
     * AppStripeProperties is expected to expose a mapping from plan name strings
     * (matching PlanType enum constant names) to Stripe Price IDs. This lookup
     * iterates
     * all known PlanType values and returns the first one whose configured price ID
     * matches the given priceId argument.
     *
     * @param priceId The Stripe Price ID to look up (e.g. "price_1Abc123...").
     * @return The matching PlanType, or null if no configured plan maps to this
     *         price ID.
     */
    private PlanType resolvePlanFromPriceId(String priceId) {
        return Arrays.stream(PlanType.values())
                .filter(plan -> priceId.equals(stripeProperties.getPriceIdForPlan(plan.name())))
                .findFirst()
                .orElse(null);
    }

    /**
     * Extracts the Stripe Customer ID from a raw Stripe event without relying on
     * the SDK's automatic deserialization.
     *
     * Different event types embed the customer ID in different object types
     * (Session,
     * Invoice, Subscription). This method inspects the event type prefix to
     * determine
     * which model to parse, then returns the customer field from that object.
     *
     * Returns null (rather than throwing) so the caller can decide how to handle
     * events with no resolvable customer, keeping the main processing pipeline
     * clean.
     *
     * @param event The verified Stripe event.
     * @return The Stripe Customer ID string (e.g. "cus_Abc123..."), or null if it
     *         could not be determined.
     */
    private String extractCustomerId(Event event) {
        String rawJson = event.getDataObjectDeserializer().getRawJson();
        if (rawJson == null)
            return null;
        try {
            String type = event.getType();
            if (type.startsWith("checkout.session")) {
                return ApiResource.GSON.fromJson(rawJson, Session.class).getCustomer();
            }
            if (type.startsWith("invoice")) {
                return ApiResource.GSON.fromJson(rawJson, Invoice.class).getCustomer();
            }
            if (type.startsWith("customer.subscription")) {
                return ApiResource.GSON.fromJson(rawJson, Subscription.class).getCustomer();
            }
        } catch (Exception ex) {
            log.warn("Failed to extract customerId from event={} type={}", event.getId(), event.getType());
        }
        return null;
    }
}