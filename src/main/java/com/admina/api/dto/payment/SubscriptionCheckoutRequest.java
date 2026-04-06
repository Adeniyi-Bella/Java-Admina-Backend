
package com.admina.api.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SubscriptionCheckoutRequest(
        @NotBlank
        @Pattern(regexp = "https://app\\.admina\\.com.*", message = "Invalid return URL")
        String returnUrl
) {}