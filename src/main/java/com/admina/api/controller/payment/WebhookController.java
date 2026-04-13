package com.admina.api.controller.payment;

import com.admina.api.service.payment.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhook Controller", description = "Stripe webhook endpoints")
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/stripe")
    @Operation(summary = "Handle Stripe webhook events")
    @ApiResponse(responseCode = "200", description = "Event processed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid payload or signature")
    @ApiResponse(responseCode = "500", description = "Internal processing error")
    public ResponseEntity<Void> handleStripeWebhook(
            @RequestBody byte[] payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        webhookService.handleStripeWebhook(payload, sigHeader);
        return ResponseEntity.ok().build();
    }
}