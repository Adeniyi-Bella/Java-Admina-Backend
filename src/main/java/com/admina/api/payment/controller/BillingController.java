
package com.admina.api.payment.controller;

import com.admina.api.payment.dto.PaymentUrlResponse;
import com.admina.api.payment.dto.SubscriptionCheckoutRequest;
import com.admina.api.payment.services.BillingService;
import com.admina.api.security.auth.AuthenticatedPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
@Tag(name = "Billing Controller")
public class BillingController {

    private final BillingService billingService;

    @PostMapping("/checkout")
    @Operation(summary = "Create Stripe checkout session for plan upgrade", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Checkout session created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid plan or return URL")
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token")
    @ApiResponse(responseCode = "402", description = "Card declined")
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "409", description = "Duplicate request or already on this plan")
    @ApiResponse(responseCode = "500", description = "Stripe configuration error or internal failure")
    @ApiResponse(responseCode = "503", description = "Stripe service unavailable or rate limited")
    public ResponseEntity<PaymentUrlResponse> createCheckout(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @RequestBody @Valid SubscriptionCheckoutRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {

        String url = billingService.createCheckoutSession(principal, request.planType(),
                idempotencyKey);
        return ResponseEntity.ok(new PaymentUrlResponse(url));
    }

    @PostMapping("/portal")
    @Operation(summary = "Create Stripe customer portal session for managing subscription", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Portal session created successfully")
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token")
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "400", description = "No active billing account")
    @ApiResponse(responseCode = "500", description = "Stripe configuration error or internal failure")
    @ApiResponse(responseCode = "503", description = "Stripe service unavailable or rate limited")
    public ResponseEntity<PaymentUrlResponse> createPortal(
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {

        String url = billingService.createPortalSession(principal);
        return ResponseEntity.ok(new PaymentUrlResponse(url));
    }
}
