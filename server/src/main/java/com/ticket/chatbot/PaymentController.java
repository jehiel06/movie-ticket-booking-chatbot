package com.ticket.chatbot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.chatbot.model.Booking;
import com.ticket.chatbot.repositories.BookingRepository;
import com.ticket.chatbot.service.CashfreePaymentService;
import com.ticket.chatbot.service.EmailService;
import org.apache.commons.codec.digest.HmacUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/payment")
public class PaymentController {
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final CashfreePaymentService paymentService;
    @Value("${cashfree.webhook.secret}")
    private String webhookSecret;

    public PaymentController(CashfreePaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String rawPayload,
            @RequestHeader("x-cf-signature") String signature) {

        try {
            // 1. Verify signature
            String computedSignature = HmacUtils.hmacSha256Hex(webhookSecret, rawPayload);
            if (!computedSignature.equals(signature)) {
                logger.error("Invalid webhook signature");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // 2. Parse payload
            Map<String, Object> payload = new ObjectMapper().readValue(rawPayload, Map.class);
            String orderId = (String) payload.get("order_id");
            String status = (String) payload.get("order_status");

            logger.info("Received webhook for order {} with status {}", orderId, status);

            // 3. Handle payment status
            if ("PAID".equals(status)) {
                paymentService.handleSuccessfulPayment(orderId, payload);
                return ResponseEntity.ok("WEBHOOK_PROCESSED");
            }

            return ResponseEntity.ok("Status ignored: " + status);

        } catch (Exception e) {
            logger.error("Webhook processing error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}