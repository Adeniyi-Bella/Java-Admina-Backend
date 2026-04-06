package com.admina.api.service.payment;

import com.admina.api.config.properties.AppPlanProperties;
import com.admina.api.config.properties.AppStripeProperties;
import com.admina.api.dto.payment.SubscriptionCheckoutRequest;
import com.admina.api.dto.payment.SubscriptionCheckoutResponse;
import com.admina.api.dto.user.UserDto;
import com.admina.api.enums.PaymentIdempotencyStatus;
import com.admina.api.enums.PlanType;
import com.admina.api.exceptions.AppExceptions;
import com.admina.api.redis.RedisKeys;
import com.admina.api.redis.RedisService;
import com.admina.api.security.AuthenticatedPrincipal;
import com.admina.api.service.user.UserService;
import com.stripe.exception.ApiConnectionException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.IdempotencyException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.RateLimitException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {

    private final UserService userService;
    private final RedisService redisService;
    private final AppStripeProperties stripeProperties;
    private final AppPlanProperties planProperties;

    @Override
    public SubscriptionCheckoutResponse createCheckoutSession(
            AuthenticatedPrincipal principal,
            String planParam,
            SubscriptionCheckoutRequest request,
            String idempotencyKey) {

        // 1. Validate plan
        PlanType plan;
        try {
            plan = PlanType.valueOf(planParam.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppExceptions.BadRequestException("Invalid plan: " + planParam);
        }

        if (plan == PlanType.FREE) {
            throw new AppExceptions.BadRequestException("Cannot subscribe to FREE plan");
        }

        planProperties.getMaxForPlan(plan);

        // 2. Idempotency lock — set BEFORE any external calls
        String redisKey = RedisKeys.stripePaymentIdempotency(idempotencyKey);
        if (redisService.hasKey(redisKey)) {
            log.warn("Duplicate subscription request idempotencyKey={} email={}", idempotencyKey, principal.getEmail());
            throw new AppExceptions.ConflictException("Subscription request already in progress");
        }
        redisService.setPaymentIdempotencyStatus(redisKey, PaymentIdempotencyStatus.PENDING, null,
                Duration.ofHours(24));

        // 3. Get user
        UserDto user = userService.getExistingUserByEmail(principal.getEmail());

        // 4. Duplicate plan check
        if (user.plan() == plan) {
            redisService.deleteKey(redisKey);
            throw new AppExceptions.ConflictException("Already subscribed to " + plan);
        }

        // 5. Resolve or create Stripe customer
        String customerId = resolveStripeCustomer(user, principal.getEmail(), redisKey);

        // 6. Create Stripe checkout session
        String priceId = stripeProperties.getPriceIdForPlan(plan.name());
        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomer(customerId)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPrice(priceId)
                                    .setQuantity(1L)
                                    .build())
                    .setSuccessUrl(request.returnUrl() + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(request.returnUrl() + "?canceled=true")
                    .putMetadata("email", principal.getEmail())
                    .putMetadata("plan", plan.name())
                    .build();

            Session session = Session.create(params);

            redisService.setPaymentIdempotencyStatus(redisKey, PaymentIdempotencyStatus.COMPLETED, session.getId(),
                    Duration.ofHours(24));
            log.info("Created Stripe session={} user={} plan={}", session.getId(), principal.getEmail(), plan);

            return new SubscriptionCheckoutResponse(session.getId(), session.getUrl());

        } catch (CardException e) {
            redisService.deleteKey(redisKey);
            log.warn("Card declined user={} code={}", principal.getEmail(), e.getCode());
            throw new AppExceptions.PaymentException("Card declined: " + e.getUserMessage());

        } catch (InvalidRequestException e) {
            redisService.deleteKey(redisKey);
            log.error("Invalid Stripe request user={} error={}", principal.getEmail(), e.getMessage());
            throw new AppExceptions.InternalServerErrorException("Payment configuration error");

        } catch (AuthenticationException e) {
            redisService.deleteKey(redisKey);
            log.error("Stripe authentication failed — check API key user={}", principal.getEmail());
            throw new AppExceptions.InternalServerErrorException("Payment setup failed");

        } catch (RateLimitException e) {
            redisService.deleteKey(redisKey);
            log.warn("Stripe rate limit hit user={}", principal.getEmail());
            throw new AppExceptions.ServiceUnavailableException("Payment service busy, please retry");

        } catch (ApiConnectionException e) {
            redisService.deleteKey(redisKey);
            log.warn("Stripe network error user={} error={}", principal.getEmail(), e.getMessage());
            throw new AppExceptions.ServiceUnavailableException("Payment service unavailable, please retry");

        } catch (IdempotencyException e) {
            redisService.deleteKey(redisKey);
            log.error("Idempotency conflict user={} error={}", principal.getEmail(), e.getMessage());
            throw new AppExceptions.InternalServerErrorException("Payment setup failed");

        } catch (StripeException e) {
            redisService.deleteKey(redisKey);
            log.error("Stripe error user={} error={}", principal.getEmail(), e.getMessage());
            throw new AppExceptions.InternalServerErrorException("Payment setup failed");
        }
    }

    private String resolveStripeCustomer(UserDto user, String email, String redisKey) {
        if (user.stripeCustomerId() != null) {
            return user.stripeCustomerId();
        }
        try {
            CustomerCreateParams params = CustomerCreateParams.builder()
                    .setEmail(email)
                    .putMetadata("user_id", user.id().toString())
                    .putMetadata("username", user.username())
                    .build();
            Customer customer = Customer.create(params);
            userService.updateStripeCustomerId(email, customer.getId());
            log.info("Created Stripe customer={} for user={}", customer.getId(), email);
            return customer.getId();
        } catch (StripeException e) {
            redisService.deleteKey(redisKey);
            log.error("Failed to create Stripe customer user={} error={}", email, e.getMessage());
            throw new AppExceptions.InternalServerErrorException("Payment setup failed");
        }
    }
}