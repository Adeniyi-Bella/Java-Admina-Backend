
package com.admina.api.payment.services;

import com.admina.api.security.auth.AuthenticatedPrincipal;
import com.admina.api.user.enums.PlanType;

public interface BillingService {
    String createCheckoutSession(
            AuthenticatedPrincipal principal,
            PlanType targetPlan,
            String idempotencyKey);

    String createPortalSession(AuthenticatedPrincipal principal);

}
