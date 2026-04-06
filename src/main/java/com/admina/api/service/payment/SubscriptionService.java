
package com.admina.api.service.payment;

import com.admina.api.dto.payment.SubscriptionCheckoutResponse;
import com.admina.api.security.AuthenticatedPrincipal;
import com.admina.api.dto.payment.SubscriptionCheckoutRequest;

public interface SubscriptionService {
    SubscriptionCheckoutResponse createCheckoutSession(
            AuthenticatedPrincipal principal,
            String plan,
            SubscriptionCheckoutRequest request,
            String idempotencyKey
    );
}

