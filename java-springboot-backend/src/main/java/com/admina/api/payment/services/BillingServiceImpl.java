package com.admina.api.payment.services;

import com.admina.api.config.properties.AppStripeProperties;
import com.admina.api.exceptions.AppExceptions;
import com.admina.api.security.auth.AuthenticatedPrincipal;
import com.admina.api.user.dto.UserDto;
import com.admina.api.user.enums.PlanType;
import com.admina.api.user.service.UserService;
import com.stripe.StripeClient;
import com.stripe.exception.ApiConnectionException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.IdempotencyException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.RateLimitException;
import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import com.stripe.model.billingportal.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingServiceImpl implements BillingService {

    private final UserService userService;
    private final AppStripeProperties stripeProperties;
    private final StripeClient stripeClient;

    @Override
    public String createCheckoutSession(
            AuthenticatedPrincipal principal,
            PlanType targetPlan,
            String idempotencyKey) {

        if (targetPlan == PlanType.FREE) {
            throw new AppExceptions.BadRequestException("Cannot subscribe to FREE plan");
        }

        UserDto user = userService.getExistingUserByEmail(principal.getEmail());

        if (user.plan() == targetPlan) {
            throw new AppExceptions.ConflictException("Already subscribed to " + targetPlan);
        }

        String priceId = stripeProperties.getPriceIdForPlan(targetPlan.name());
        if (priceId == null) {
            throw new AppExceptions.InternalServerErrorException("No price configured for plan: " + targetPlan);
        }

        // If user already has a subscription, update it instead of creating a new
        // checkout
        if (user.stripeSubscriptionId() != null) {
            return updateExistingSubscription(user, principal.getEmail(), priceId, targetPlan, idempotencyKey);
        }

        // First time — create checkout session as before
        String customerId = resolveStripeCustomer(user, principal.getEmail());
        return createNewCheckoutSession(customerId, priceId, targetPlan, principal.getEmail(), idempotencyKey);
    }

    private String updateExistingSubscription(UserDto user, String email, String priceId, PlanType targetPlan,
            String idempotencyKey) {
        try {
            Subscription subscription = stripeClient.v1().subscriptions()
                    .retrieve(user.stripeSubscriptionId());

            String existingItemId = subscription.getItems().getData().get(0).getId();

            SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                    .addItem(
                            SubscriptionUpdateParams.Item.builder()
                                    .setId(existingItemId)
                                    .setPrice(priceId)
                                    .build())
                    // ALWAYS_INVOICE creates and immediately finalizes an invoice
                    // for the prorated difference — charging the user right away
                    .setProrationBehavior(SubscriptionUpdateParams.ProrationBehavior.ALWAYS_INVOICE)
                    // Charge the proration invoice immediately rather than waiting
                    // for the next billing cycle
                    .setPaymentBehavior(SubscriptionUpdateParams.PaymentBehavior.PENDING_IF_INCOMPLETE)
                    .build();

            RequestOptions options = RequestOptions.builder().setIdempotencyKey(idempotencyKey).build();

            stripeClient.v1().subscriptions().update(user.stripeSubscriptionId(), params, options);

            log.info("Updated subscription for user={} newPlan={}", email, targetPlan);

            // Return user to the checkout URL after update
            return stripeProperties.checkoutUrl() + "?upgraded=true";

        } catch (StripeException e) {
            log.error("Failed to update subscription user={} error={}", email, e.getMessage());
            throw new AppExceptions.InternalServerErrorException("Failed to update subscription");
        }
    }

    private String createNewCheckoutSession(String customerId, String priceId, PlanType targetPlan, String email,
            String idempotencyKey) {
        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomer(customerId)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPrice(priceId)
                                    .setQuantity(1L)
                                    .build())
                    .setSuccessUrl(stripeProperties.checkoutUrl() + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(stripeProperties.checkoutUrl() + "?canceled=true")
                    .putMetadata("email", email)
                    .putMetadata("plan", targetPlan.name())
                    .build();

            RequestOptions options = RequestOptions.builder().setIdempotencyKey(idempotencyKey).build();

            log.info("Created Stripe checkout user={} plan={}", email, targetPlan);

            return stripeClient.v1().checkout().sessions().create(params, options).getUrl();

        } catch (CardException e) {
            log.warn("Card declined user={} code={}", email, e.getCode());
            throw new AppExceptions.PaymentException("Card declined: " + e.getUserMessage());
        } catch (InvalidRequestException e) {
            log.error("Invalid Stripe request user={} error={}", email, e.getMessage());
            throw new AppExceptions.InternalServerErrorException("Payment configuration error");
        } catch (AuthenticationException e) {
            log.error("Stripe authentication failed user={}", email);
            throw new AppExceptions.InternalServerErrorException("Payment setup failed");
        } catch (RateLimitException e) {
            log.warn("Stripe rate limit hit user={}", email);
            throw new AppExceptions.ServiceUnavailableException("Payment service busy, please retry");
        } catch (ApiConnectionException e) {
            log.warn("Stripe network error user={} error={}", email, e.getMessage());
            throw new AppExceptions.ServiceUnavailableException("Payment service unavailable, please retry");
        } catch (IdempotencyException e) {
            log.error("Idempotency conflict user={} error={}", email, e.getMessage());
            throw new AppExceptions.InternalServerErrorException("Payment setup failed");
        } catch (StripeException e) {
            log.error("Stripe error user={} error={}", email, e.getMessage());
            throw new AppExceptions.InternalServerErrorException("Payment setup failed");
        }
    }

    @Override
    public String createPortalSession(AuthenticatedPrincipal principal) {
        UserDto user = userService.getExistingUserByEmail(principal.getEmail());

        if (user.stripeCustomerId() == null) {
            throw new AppExceptions.BadRequestException(
                    "No active billing account found");
        }

        try {
            com.stripe.param.billingportal.SessionCreateParams params = com.stripe.param.billingportal.SessionCreateParams
                    .builder()
                    .setCustomer(user.stripeCustomerId())
                    .setReturnUrl(stripeProperties.checkoutUrl())
                    .build();

            Session portalSession = stripeClient.v1().billingPortal()
                    .sessions()
                    .create(params);

            log.info("Created billing portal session for user={}", principal.getEmail());
            return portalSession.getUrl();

        } catch (ApiConnectionException e) {
            log.warn("Stripe network error user={} error={}", principal.getEmail(), e.getMessage());
            throw new AppExceptions.ServiceUnavailableException(
                    "Payment service unavailable, please retry");

        } catch (RateLimitException e) {
            log.warn("Stripe rate limit hit user={}", principal.getEmail());
            throw new AppExceptions.ServiceUnavailableException(
                    "Payment service busy, please retry");

        } catch (AuthenticationException e) {
            log.error("Stripe authentication failed user={}", principal.getEmail());
            throw new AppExceptions.InternalServerErrorException("Payment setup failed");

        } catch (StripeException e) {
            log.error("Stripe portal error user={} error={}", principal.getEmail(), e.getMessage());
            throw new AppExceptions.InternalServerErrorException("Payment setup failed");
        }
    }

    private String resolveStripeCustomer(UserDto user, String email) {
        if (user.stripeCustomerId() != null) {
            return user.stripeCustomerId();
        }
        try {
            CustomerCreateParams params = CustomerCreateParams.builder()
                    .setEmail(email)
                    .setName(user.username())
                    .putMetadata("user_id", user.id().toString())
                    .putMetadata("username", user.username())
                    .build();
            com.stripe.model.Customer customer = stripeClient.v1().customers().create(params);
            userService.updateStripeCustomerId(email, customer.getId());
            return customer.getId();
        } catch (StripeException e) {
            log.error("Failed to create Stripe customer user={} error={}", email, e.getMessage());
            throw new AppExceptions.InternalServerErrorException("Payment setup failed");
        }
    }
}
