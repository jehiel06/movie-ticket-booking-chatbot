package com.ticket.chatbot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class CashfreeRestClient {
    @Value("${cashfree.app.id}")
    private String appId;

    @Value("${cashfree.secret.key}")
    private String secretKey;

    @Value("${cashfree.base.url}")
    private String baseUrl;

    @Value("${cashfree.test.mode}")
    private boolean testMode;

    private final RestTemplate restTemplate = new RestTemplate();

    public String createPaymentLink(String orderId, double amount,
                                    String customerEmail, String description) {
        String url = baseUrl + "/orders";

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-client-id", appId);
        headers.set("x-client-secret", secretKey);
        headers.set("x-api-version", "2022-09-01");
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Build order meta with proper webhook URL
        Map<String, Object> orderMeta = new HashMap<>();
        orderMeta.put("notify_url", "https://your-ngrok-url.ngrok-free.app/payment/webhook");
        orderMeta.put("return_url", "https://your-ngrok-url.ngrok-free.app/payment/return");

        Map<String, Object> request = new HashMap<>();
        request.put("order_id", orderId);
        request.put("order_amount", amount);
        request.put("order_currency", "INR");
        request.put("order_note", description);
        request.put("order_meta", orderMeta); // Include the webhook URL
        request.put("customer_details", Map.of(
                "customer_id", "cust_" + orderId,
                "customer_email", customerEmail,
                "customer_phone", "7092412303"
        ));
        request.put("payment_methods", "card,upi");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (response.getStatusCode().is2xxSuccessful() && responseBody != null) {
                String paymentLink = Optional.ofNullable(responseBody.get("payment_link"))
                        .orElseGet(() -> ((Map<String, Object>)responseBody.get("payments")).get("url"))
                        .toString();

               // logger.info("Generated payment link for order {}: {}", orderId, paymentLink);
                return paymentLink;
            }
            throw new RuntimeException("API Error: " + response.getBody());
        } catch (Exception e) {
//            logger.error("Failed to create payment link for order {}", orderId, e);
            throw new RuntimeException("Payment gateway error", e);
        }
    }
    public boolean isTestMode() {
        return testMode;
    }
}