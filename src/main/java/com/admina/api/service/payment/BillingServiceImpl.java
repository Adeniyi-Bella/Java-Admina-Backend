package com.admina.api.service.payment;

import com.admina.api.config.properties.AppStripeProperties;
import com.admina.api.dto.user.UserDto;
import com.admina.api.enums.PlanType;
import com.admina.api.exceptions.AppExceptions;
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
import com.stripe.net.RequestOptions;
import com.stripe.param.CustomerCreateParams;
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

    @Override
    public String createCheckoutSession(
            AuthenticatedPrincipal principal,
            PlanType targetPlan,
            String idempotencyKey) {

        // 1. Validate plan
        if (targetPlan == PlanType.FREE) {
            throw new AppExceptions.BadRequestException("Cannot subscribe to FREE plan");
        }

        // 3. Get user
        UserDto user = userService.getExistingUserByEmail(principal.getEmail());

        // 4. Duplicate plan check
        if (user.plan() == targetPlan) {
            throw new AppExceptions.ConflictException("Already subscribed to " + targetPlan);
        }

        // 5. Resolve or create Stripe customer
        String customerId = resolveStripeCustomer(user, principal.getEmail());

        // 6. Create Stripe checkout session
        String priceId = stripeProperties.getPriceIdForPlan(targetPlan.name());
        
        if (priceId == null) {
            throw new AppExceptions.InternalServerErrorException("No price configured for plan: " + targetPlan);
        }

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
                    .putMetadata("email", principal.getEmail())
                    .putMetadata("plan", targetPlan.name())
                    .build();

            RequestOptions options = RequestOptions.builder().setIdempotencyKey(idempotencyKey).build();

            log.info("Created Stripe user={} plan={}", principal.getEmail(), targetPlan);

            return Session.create(params, options).getUrl();

        } catch (CardException e) {
            log.warn("Card declined user={} code={}", principal.getEmail(), e.getCode());
            throw new AppExceptions.PaymentException("Card declined: " + e.getUserMessage());

        } catch (InvalidRequestException e) {
            log.error("Invalid Stripe request user={} error={}", principal.getEmail(), e.getMessage());
            throw new AppExceptions.InternalServerErrorException("Payment configuration error");

        } catch (AuthenticationException e) {
            log.error("Stripe authentication failed — check API key user={}", principal.getEmail());
            throw new AppExceptions.InternalServerErrorException("Payment setup failed");

        } catch (RateLimitException e) {
            log.warn("Stripe rate limit hit user={}", principal.getEmail());
            throw new AppExceptions.ServiceUnavailableException("Payment service busy, please retry");

        } catch (ApiConnectionException e) {
            log.warn("Stripe network error user={} error={}", principal.getEmail(), e.getMessage());
            throw new AppExceptions.ServiceUnavailableException("Payment service unavailable, please retry");

        } catch (IdempotencyException e) {
            log.error("Idempotency conflict user={} error={}", principal.getEmail(), e.getMessage());
            throw new AppExceptions.InternalServerErrorException("Payment setup failed");

        } catch (StripeException e) {
            log.error("Stripe error user={} error={}", principal.getEmail(), e.getMessage());
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

            com.stripe.model.billingportal.Session portalSession = com.stripe.model.billingportal.Session
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
            Customer customer = Customer.create(params);
            userService.updateStripeCustomerId(email, customer.getId());
            return customer.getId();
        } catch (StripeException e) {
            log.error("Failed to create Stripe customer user={} error={}", email, e.getMessage());
            throw new AppExceptions.InternalServerErrorException("Payment setup failed");
        }
    }
}