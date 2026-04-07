
package com.admina.api.controller;

import com.admina.api.dto.payment.SubscriptionCheckoutRequest;
import com.admina.api.dto.payment.SubscriptionCheckoutResponse;
import com.admina.api.security.AuthenticatedPrincipal;
import com.admina.api.service.payment.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscription Controller", description = "Stripe subscription APIs")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/{plan}")
    @Operation(summary = "Create Stripe checkout session for plan upgrade", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Checkout session created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid plan or return URL")
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token")
    @ApiResponse(responseCode = "402", description = "Card declined")
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "409", description = "Duplicate request or already on this plan")
    @ApiResponse(responseCode = "500", description = "Stripe configuration error or internal failure")
    @ApiResponse(responseCode = "503", description = "Stripe service unavailable or rate limited")
    public ResponseEntity<SubscriptionCheckoutResponse> createSubscription(
            @PathVariable String plan,
            @RequestBody @Valid SubscriptionCheckoutRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @RequestHeader("Stripe-Payment-Idempotency-Key") String idempotencyKey) {

        SubscriptionCheckoutResponse response = subscriptionService.createCheckoutSession(principal, plan, request,
                idempotencyKey);
        return ResponseEntity.ok(response);
    }
}
