
package com.admina.api.service.payment;

import com.admina.api.security.AuthenticatedPrincipal;
import com.admina.api.enums.PlanType;

public interface BillingService {
    String createCheckoutSession(
            AuthenticatedPrincipal principal,
            PlanType targetPlan,
            String idempotencyKey);

    String createPortalSession(AuthenticatedPrincipal principal);

}
