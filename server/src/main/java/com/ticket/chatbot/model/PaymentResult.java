package com.ticket.chatbot.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class PaymentResult {
    private boolean success;
    private String message;
    private String paymentId;
    private String bookingId;
    private LocalDateTime timestamp;

    public PaymentResult(boolean success, String message, String paymentId, String bookingId) {
        this.success = success;
        this.message = message;
        this.paymentId = paymentId;
        this.bookingId = bookingId;
        this.timestamp = LocalDateTime.now();
    }

    public static PaymentResult success(String paymentId, String bookingId) {
        return new PaymentResult(true, "Success", paymentId, bookingId);
    }

    public static PaymentResult failure(String message) {
        return new PaymentResult(false, message, null, null);
    }
}
