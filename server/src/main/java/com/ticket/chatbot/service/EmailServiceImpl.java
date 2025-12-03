package com.ticket.chatbot.service;

import com.ticket.chatbot.model.Booking;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Optional;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    private JavaMailSender mailSender;

    @Override
    public void sendTicketConfirmation(String recipientEmail,
                                       String bookingId,
                                       String movieTitle,
                                       LocalDateTime showTime,
                                       List<String> seatNumbers,
                                       Double totalAmount,
                                       String paymentLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(recipientEmail);
            helper.setSubject("Your Ticket Booking Confirmation - Order #" + bookingId);

            String emailContent = buildEmailContent(bookingId, movieTitle, showTime, seatNumbers, totalAmount, paymentLink);
            helper.setText(emailContent, true);

            mailSender.send(message);
            logger.info("Ticket confirmation email sent to: {}", recipientEmail);
        } catch (MessagingException e) {
            logger.error("Failed to send confirmation email to {}", recipientEmail, e);
            throw new RuntimeException("Failed to send confirmation email", e);
        }
    }

    @Override
    public void sendPaymentConfirmation(String recipientEmail,
                                        String bookingId,
                                        String movieTitle,
                                        LocalDateTime showTime,
                                        List<String> seatNumbers,
                                        Double totalAmount) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(recipientEmail);
            helper.setSubject("Payment Confirmed - Order #" + bookingId);

            String emailContent = buildPaymentConfirmationContent(bookingId, movieTitle, showTime, seatNumbers, totalAmount);
            helper.setText(emailContent, true);

            mailSender.send(message);
            logger.info("Payment confirmation email sent to: {}", recipientEmail);
        } catch (MessagingException e) {
            logger.error("Failed to send payment confirmation email to {}", recipientEmail, e);
            throw new RuntimeException("Failed to send payment confirmation email", e);
        }
    }

    private String buildEmailContent(String bookingId,
                                     String movieTitle,
                                     LocalDateTime showTime,
                                     List<String> seatNumbers,
                                     Double totalAmount,
                                     String paymentLink) {
        // Safe handling of potentially null values
        String safeMovieTitle = Optional.ofNullable(movieTitle).orElse("Unknown Movie");
        String safeSeats = Optional.ofNullable(seatNumbers)
                .map(list -> String.join(", ", list))
                .orElse("Seats not specified");
        Double safeAmount = Optional.ofNullable(totalAmount).orElse(0.0);

        // Safe date formatting
        String formattedShowTime = Optional.ofNullable(showTime)
                .map(time -> time.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)))
                .orElse("Time not specified");

        return "<!DOCTYPE html>" +
                "<html lang=\"en\">" +
                "<head>" +
                "    <meta charset=\"UTF-8\">" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "    <title>Ticket Confirmation</title>" +
                "    <style>" +
                "        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }" +
                "        .header { background-color: #f5f5f5; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }" +
                "        .content { padding: 20px; border: 1px solid #ddd; border-top: none; border-radius: 0 0 5px 5px; }" +
                "        .ticket-info { margin-bottom: 15px; }" +
                "        .ticket-info strong { display: inline-block; width: 120px; }" +
                "        .footer { margin-top: 20px; font-size: 12px; color: #777; text-align: center; }" +
                "        .status-pending { color: #ff9800; font-weight: bold; }" +
                "        .payment-link { margin-top: 20px; padding: 15px; background-color: #f8f9fa; border-radius: 5px; }" +
                "        .payment-button { background-color: #4CAF50; color: white; padding: 10px 15px; text-decoration: none; border-radius: 5px; display: inline-block; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class=\"header\">" +
                "        <h2>Your Movie Ticket Confirmation</h2>" +
                "    </div>" +
                "    <div class=\"content\">" +
                "        <p>Dear Customer,</p>" +
                "        <p>Thank you for your booking! Here are your ticket details:</p>" +
                "        " +
                "        <div class=\"ticket-info\">" +
                "            <strong>Order ID:</strong> " + bookingId + "<br>" +
                "            <strong>Movie:</strong> " + safeMovieTitle + "<br>" +
                "            <strong>Show Time:</strong> " + formattedShowTime + "<br>" +
                "            <strong>Seats:</strong> " + safeSeats + "<br>" +
                "            <strong>Total Amount:</strong> ₹" + String.format("%.2f", safeAmount) + "<br>" +
                "            <strong>Status:</strong> <span class=\"status-pending\">PENDING PAYMENT</span>" +
                "        </div>" +
                "        " +
                "        <div class=\"payment-link\">" +
                "            <p>Please complete your payment to confirm your booking:</p>" +
                "            <p><a href=\"" + paymentLink + "\" class=\"payment-button\">Complete Payment Now</a></p>" +
                "            <p>This link will expire in 30 minutes.</p>" +
                "        </div>" +
                "        " +
                "        <p>If you have any questions, please contact our customer support.</p>" +
                "    </div>" +
                "    " +
                "    <div class=\"footer\">" +
                "        <p>© 2025 Ticket Booking System. All rights reserved.</p>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }

    private String buildPaymentConfirmationContent(String bookingId,
                                                   String movieTitle,
                                                   LocalDateTime showTime,
                                                   List<String> seatNumbers,
                                                   Double totalAmount) {
        // Safe handling of potentially null values
        String safeMovieTitle = Optional.ofNullable(movieTitle).orElse("Unknown Movie");
        String safeSeats = Optional.ofNullable(seatNumbers)
                .map(list -> String.join(", ", list))
                .orElse("Seats not specified");
        Double safeAmount = Optional.ofNullable(totalAmount).orElse(0.0);

        // Safe date formatting
        String formattedShowTime = Optional.ofNullable(showTime)
                .map(time -> time.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)))
                .orElse("Time not specified");

        return "<!DOCTYPE html>" +
                "<html lang=\"en\">" +
                "<head>" +
                "    <meta charset=\"UTF-8\">" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "    <title>Payment Confirmation</title>" +
                "    <style>" +
                "        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }" +
                "        .header { background-color: #f5f5f5; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }" +
                "        .content { padding: 20px; border: 1px solid #ddd; border-top: none; border-radius: 0 0 5px 5px; }" +
                "        .ticket-info { margin-bottom: 15px; }" +
                "        .ticket-info strong { display: inline-block; width: 120px; }" +
                "        .footer { margin-top: 20px; font-size: 12px; color: #777; text-align: center; }" +
                "        .status-confirmed { color: #4caf50; font-weight: bold; }" +
                "        .download-button { background-color: #2196F3; color: white; padding: 10px 15px; text-decoration: none; border-radius: 5px; display: inline-block; margin-top: 10px; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class=\"header\">" +
                "        <h2>Payment Confirmed</h2>" +
                "    </div>" +
                "    <div class=\"content\">" +
                "        <p>Dear Customer,</p>" +
                "        <p>Thank you for your payment! Your booking is now confirmed.</p>" +
                "        " +
                "        <div class=\"ticket-info\">" +
                "            <strong>Order ID:</strong> " + bookingId + "<br>" +
                "            <strong>Movie:</strong> " + safeMovieTitle + "<br>" +
                "            <strong>Show Time:</strong> " + formattedShowTime + "<br>" +
                "            <strong>Seats:</strong> " + safeSeats + "<br>" +
                "            <strong>Total Paid:</strong> ₹" + String.format("%.2f", safeAmount) + "<br>" +
                "            <strong>Status:</strong> <span class=\"status-confirmed\">CONFIRMED</span>" +
                "        </div>" +
                "        " +
                "        <p>You can download your tickets here:</p>" +
                "        <a href=\"https://your-ticket-download-url/" + bookingId + "\" class=\"download-button\">Download Tickets</a>" +
                "        " +
                "        <p>Please present your tickets at the theater entrance.</p>" +
                "        " +
                "        <p>If you have any questions, please contact our customer support.</p>" +
                "    </div>" +
                "    " +
                "    <div class=\"footer\">" +
                "        <p>© 2025 Ticket Booking System. All rights reserved.</p>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }
}