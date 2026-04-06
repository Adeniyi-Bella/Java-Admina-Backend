
package com.admina.api.dto.payment;

public record SubscriptionCheckoutResponse(
        String sessionId,
        String sessionUrl
) {
}

