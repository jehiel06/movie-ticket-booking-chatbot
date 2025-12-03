package com.ticket.chatbot.service;

import java.time.LocalDateTime;
import java.util.List;

public interface EmailService {
    void sendTicketConfirmation(
            String recipientEmail,
            String bookingId,
            String movieTitle,
            LocalDateTime showTime,
            List<String> seats,
            Double amount,
            String ticketUrl
    );

    void sendPaymentConfirmation(
            String recipientEmail,
            String bookingId,
            String movieTitle,
            LocalDateTime showTime,
            List<String> seatNumbers,
            Double totalAmount
    );
}
