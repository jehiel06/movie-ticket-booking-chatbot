package com.ticket.chatbot.service;

import com.ticket.chatbot.model.Booking;
import com.ticket.chatbot.repositories.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class CashfreePaymentService {

    private static final Logger logger = LoggerFactory.getLogger(CashfreePaymentService.class);
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private EmailService emailService;

    @Value("${cashfree.payment.url}")
    private String paymentBaseUrl;

    public String createPaymentLinkAndSendTicket(String bookingId, Double amount, String purpose, String userEmail) {
        try {
            // Get the booking from repository
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

            // Generate payment link
            String paymentLink = generatePaymentLink(bookingId, amount, purpose);

            // Send email in a separate thread
            executorService.submit(() -> sendTicketEmail(booking, userEmail, paymentLink));

            return paymentLink;
        } catch (Exception e) {
            logger.error("Failed to create payment link for booking {}", bookingId, e);
            throw new RuntimeException("Failed to create payment link", e);
        }
    }

    private void sendTicketEmail(Booking booking, String userEmail, String paymentLink) {
        try {
            emailService.sendTicketConfirmation(
                    userEmail,
                    booking.getId(),
                    booking.getMovieTitle(),
                    booking.getShowTime(),
                    booking.getSeatNumbers(),
                    booking.getTotalAmount(),
                    paymentLink
            );
            logger.info("Successfully sent ticket email for order {}", booking.getId());
        } catch (Exception e) {
            logger.error("Failed to send ticket email for order {}", booking.getId(), e);
        }
    }

    @Transactional
    public void handleSuccessfulPayment(String orderId, Map<String, Object> payload) {
        try {
            Booking booking = bookingRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Booking not found: " + orderId));

            // Update booking status
            booking.setStatus("CONFIRMED");
            bookingRepository.save(booking);

            // Get customer email from payload or use booking email
            String customerEmail = Optional.ofNullable(payload.get("customer_details"))
                    .map(details -> ((Map<String, Object>) details).get("customer_email"))
                    .map(Object::toString)
                    .orElse(booking.getCustomerEmail());

            // Send confirmation email
            sendConfirmationEmail(booking, customerEmail);

        } catch (Exception e) {
            logger.error("Failed to process successful payment for order {}", orderId, e);
            throw new RuntimeException("Payment processing failed", e);
        }
    }

    private void sendConfirmationEmail(Booking booking, String customerEmail) {
        executorService.submit(() -> {
            try {
                emailService.sendPaymentConfirmation(
                        customerEmail,
                        booking.getId(),
                        booking.getMovieTitle(),
                        booking.getShowTime(),
                        booking.getSeatNumbers(),
                        booking.getTotalAmount()
                );
                logger.info("Payment confirmation email sent for order {}", booking.getId());
            } catch (Exception e) {
                logger.error("Failed to send payment confirmation email for order {}", booking.getId(), e);
            }
        });
    }

    private String generatePaymentLink(String bookingId, Double amount, String purpose) {
        return paymentBaseUrl + "?orderId=" + bookingId + "&amount=" + amount;
    }
}