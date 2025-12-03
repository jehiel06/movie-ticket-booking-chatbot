package com.ticket.chatbot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.dialogflow.v2.*;
import com.google.cloud.speech.v1.*;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.protobuf.ByteString;
import com.google.protobuf.Value;
import com.ticket.chatbot.model.Booking;
import com.ticket.chatbot.repositories.BookingRepository;
import com.ticket.chatbot.service.*;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
public class ChatbotController {

    private static final Logger logger = LoggerFactory.getLogger(ChatbotController.class);

    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private TmdbService tmdbService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Map<String, String>> sessionParameters = new HashMap<>();
    private final MovieServices movieServices;
    private final EmailService emailServices;
    private final CashfreePaymentService cashfreePaymentService;

    public ChatbotController(MovieServices movieServices,
                             BookingRepository bookingRepository,
                             CashfreePaymentService paymentService,
                             EmailService emailService) {
        this.movieServices = movieServices;
        this.bookingRepository = bookingRepository;
        this.cashfreePaymentService = paymentService;
        this.emailServices = emailService;
    }

    @GetMapping("/chat")
    public String check() {
        return "Success";
    }

    @PostMapping(value = "/chat/webhook", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public ResponseEntity<ChatbotResponse> handleWebhook(
            @RequestParam(value = "file", required = false) MultipartFile audioFile,
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "selected_seats", required = false) String selectedSeats,
            @RequestParam("sessionId") String sessionId) {
        try {
            if (selectedSeats != null && !selectedSeats.isEmpty()) {
                return handleSeatSelection(sessionParameters.getOrDefault(sessionId, new HashMap<>()),
                        sessionId,
                        selectedSeats);
            }

            String queryText;

            if (audioFile != null && !audioFile.isEmpty()) {
                System.out.println("Received audio file: " + audioFile.getOriginalFilename());
                System.out.println("Audio file size: " + audioFile.getSize() + " bytes");
                System.out.println("Audio file content type: " + audioFile.getContentType());

                queryText = convertAudioToText(audioFile);
                System.out.println("Audio Query : " + queryText);
                if (queryText == null || queryText.isEmpty()) {
                    return ResponseEntity.ok(new ChatbotResponse(List.of("Could not process the audio. Please try again.")));
                }
            } else if (query != null && !query.isEmpty()) {
                queryText = query;
            } else {
                return ResponseEntity.ok(new ChatbotResponse(List.of("Invalid input. Please provide either text or audio.")));
            }

            return processQuery(queryText, sessionId);
        } catch (Exception e) {
            System.err.println("Error processing webhook request: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(new ChatbotResponse(List.of("An error occurred while processing your request.")));
        }
    }

    private String convertAudioToText(MultipartFile audioFile) throws IOException {
        try (SpeechClient speechClient = SpeechClient.create()) {
            ByteString audioBytes = ByteString.readFrom(audioFile.getInputStream());

            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.WEBM_OPUS)
                    .setLanguageCode("en-US")
                    .setSampleRateHertz(48000)
                    .build();

            RecognitionAudio audio = RecognitionAudio.newBuilder()
                    .setContent(audioBytes)
                    .build();

            RecognizeResponse response = speechClient.recognize(config, audio);
            StringBuilder transcript = new StringBuilder();

            for (SpeechRecognitionResult result : response.getResultsList()) {
                SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                transcript.append(alternative.getTranscript());
            }
            return transcript.toString();
        }
    }

    private ResponseEntity<ChatbotResponse> processQuery(String queryText, String sessionId) throws IOException {

        if (queryText != null && (queryText.toLowerCase().contains("start over") ||
                queryText.toLowerCase().contains("reset") ||
                queryText.toLowerCase().contains("restart"))) {
            clearSessionParameters(sessionId);
            return ResponseEntity.ok(new ChatbotResponse(List.of(
                    "Okay, let's start fresh. How can I help you today?"
            )));
        }

        Map<String, String> parameters = sessionParameters.getOrDefault(sessionId, new HashMap<>());

        if ("true".equals(parameters.get("retryDateMode"))) {
            parameters.put("date", queryText); // Treat the user's response as a new date
            parameters.remove("retryDateMode");
            return handleBookTicketIntent(parameters, sessionId, queryText);
        }

        QueryResult queryResult = detectIntent(queryText, sessionId);
        String intentDisplayName = queryResult.getIntent().getDisplayName();
        Map<String, Value> fieldsMap = queryResult.getParameters().getFieldsMap();

        // Extract parameters from Dialogflow
        for (Map.Entry<String, Value> entry : fieldsMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().getStringValue();
            if (!value.isEmpty()) {
                parameters.put(key, value);
            }
        }

        sessionParameters.put(sessionId, parameters);

        System.out.println("Intent: " + intentDisplayName);
        System.out.println("Parameters: " + parameters);

        return switch (intentDisplayName) {
            case "BookTicket" -> handleBookTicketIntent(parameters, sessionId, queryText);
            case "SelectShowtime" -> handleShowtimeSelection(parameters, sessionId, queryText);
            case "movie.details" -> handleMovieDetailsIntent(parameters, sessionId);
            case "Greetings" -> handleWelcomeIntent();
            case "FallbackIntent" -> handleFallbackIntent();
            default -> handleUnknownIntent();
        };
    }

    private QueryResult detectIntent(String queryText, String sessionId) throws IOException {
        try (SessionsClient sessionsClient = SessionsClient.create()) {
            String projectId = "steam-shape-449011-a1";
            SessionName session = SessionName.of(projectId, sessionId);

            TextInput.Builder textInput = TextInput.newBuilder()
                    .setText(queryText)
                    .setLanguageCode("en-US");

            QueryInput queryInput = QueryInput.newBuilder()
                    .setText(textInput)
                    .build();

            DetectIntentResponse response = sessionsClient.detectIntent(session, queryInput);
            return response.getQueryResult();
        }
    }

    private ResponseEntity<ChatbotResponse> handleWelcomeIntent() {
        String fulfillmentText = "Hello! How can I assist you today?";
        return ResponseEntity.ok(new ChatbotResponse(List.of(fulfillmentText)));
    }

    private ResponseEntity<ChatbotResponse> handleBookTicketIntent(Map<String, String> parameters,
                                                                   String sessionId,
                                                                   String queryText) {
        // Check for reset commands
        if (queryText != null && (queryText.toLowerCase().contains("start over") ||
                queryText.toLowerCase().contains("reset") ||
                queryText.toLowerCase().contains("restart"))) {
            sessionParameters.remove(sessionId); // Clear all parameters
            return ResponseEntity.ok(new ChatbotResponse(List.of(
                    "Okay, let's start over. How can I help you today?"
            )));
        }

        String movieName = parameters.getOrDefault("movie", "");
        String theatre = parameters.getOrDefault("theatre", "");
        String location = parameters.getOrDefault("geo-city", "");
        String date = parameters.getOrDefault("date", "");
        String ticketCountStr = parameters.getOrDefault("count", "");

        // Validate input parameters
        if (movieName.isEmpty()) {
            return ResponseEntity.ok(new ChatbotResponse(List.of("Which movie would you like to book?")));
        }
        if (location.isEmpty()) {
            return ResponseEntity.ok(new ChatbotResponse(List.of("Which location should I book for?")));
        }
        if (date.isEmpty()) {
            return ResponseEntity.ok(new ChatbotResponse(List.of("What date would you like to book for?")));
        }

        // Validate ticket count
        int ticketCount = 0;
        if (!ticketCountStr.isEmpty()) {
            try {
                ticketCount = Integer.parseInt(ticketCountStr);
                if (ticketCount <= 0) {
                    return ResponseEntity.ok(new ChatbotResponse(List.of(
                            "Please enter a valid number of tickets (greater than 0).")));
                }
            } catch (NumberFormatException e) {
                return ResponseEntity.ok(new ChatbotResponse(List.of(
                        "Invalid number of tickets. Please enter a valid number.")));
            }
        } else {
            return ResponseEntity.ok(new ChatbotResponse(List.of(
                    "How many tickets would you like to book?")));
        }
        String formattedDate = FormDate(date);
        parameters.put("date", formattedDate);

        // Fetch showtimes - try with and without theatre filter
        List<Map<String, Object>> showtimes = new ArrayList<>();

        // First try with theatre filter if provided
        if (!theatre.isEmpty()) {
            showtimes = movieServices.getShowtimes(movieName, location, formattedDate, theatre);
        }

        if (showtimes.isEmpty()) {
            String fulfillmentText = "No available shows found for the given details.";
            sessionParameters.remove(sessionId);
            return ResponseEntity.ok(new ChatbotResponse(List.of(fulfillmentText)));
        }

        StringBuilder showTimesBuilder = new StringBuilder("<b>Available showtimes:</b><br>");
        for (int i = 0; i < showtimes.size(); i++) {
            Map<String, Object> show = showtimes.get(i);
            String cinema = (String) show.get("cinema");
            String time = (String) show.get("time");
            String format = show.get("format") != null ? (String) show.get("format") : "Standard";

            showTimesBuilder.append("<br>")
                    .append(i + 1).append(") ")
                    .append(time).append(" at ")
                    .append(cinema).append(" [").append(format).append("]");
        }

        parameters.put("showTimes", new Gson().toJson(showtimes));
        parameters.put("awaitingShowTimeSelection", "true");
        sessionParameters.put(sessionId, parameters);

        return ResponseEntity.ok(new ChatbotResponse(List.of(
                showTimesBuilder.toString(),
                "<br>Please select a show time by entering the corresponding number."
        )));
    }

    private ResponseEntity<ChatbotResponse> handleShowtimeSelection(Map<String, String> parameters, String sessionId, String queryText) {
        // Validate required parameters
        String selectedShowTimeNumber = parameters.get("showtime_number");
        String theatre = parameters.get("theatre");

        if (selectedShowTimeNumber == null || selectedShowTimeNumber.isEmpty()) {
            return ResponseEntity.ok(new ChatbotResponse(List.of(
                    "Please select a valid show time by entering the corresponding number."
            )));
        }

        try {
            int selectedShowTimeIndex = Integer.parseInt(selectedShowTimeNumber) - 1;
            if (selectedShowTimeIndex < 0) {
                return ResponseEntity.ok(new ChatbotResponse(List.of(
                        "Invalid selection. Please enter a positive number."
                )));
            }

            // Retrieve and parse showtimes from session
            Map<String, String> sessionParams = sessionParameters.get(sessionId);
            if (sessionParams == null) {
                return ResponseEntity.ok(new ChatbotResponse(List.of(
                        "Session expired. Please start over."
                )));
            }

            String showTimesJson = sessionParams.get("showTimes");
            if (showTimesJson == null || showTimesJson.isEmpty()) {
                return ResponseEntity.ok(new ChatbotResponse(List.of(
                        "Showtime data not found. Please start over."
                )));
            }

            List<Map<String, Object>> allShowTimes = new Gson().fromJson(
                    showTimesJson,
                    new TypeToken<List<Map<String, Object>>>() {}.getType()
            );

            if (allShowTimes == null || allShowTimes.isEmpty()) {
                return ResponseEntity.ok(new ChatbotResponse(List.of(
                        "No showtimes available. Please try again later."
                )));
            }

            if (theatre != null && !theatre.isEmpty()) {
                allShowTimes = allShowTimes.stream()
                        .filter(show -> show != null &&
                                show.get("cinema") != null &&
                                show.get("cinema").toString().toLowerCase()
                                        .contains(theatre.toLowerCase()))
                        .collect(Collectors.toList());

                if (allShowTimes.isEmpty()) {
                    return ResponseEntity.ok(new ChatbotResponse(List.of(
                            "No showtimes found for the specified theatre."
                    )));
                }
            }

            if (selectedShowTimeIndex >= allShowTimes.size()) {
                return ResponseEntity.ok(new ChatbotResponse(List.of(
                        "Invalid selection. Please choose between 1 and " + allShowTimes.size()
                )));
            }

            // Get selected showtime
            Map<String, Object> selectedShowTime = allShowTimes.get(selectedShowTimeIndex);
            if (selectedShowTime == null) {
                return ResponseEntity.ok(new ChatbotResponse(List.of(
                        "Selected showtime not found. Please try again."
                )));
            }

            String time = selectedShowTime.get("time") != null ?
                    selectedShowTime.get("time").toString() : "Time not specified";

            String showtimeKey = selectedShowTime.get("showtimeKey") != null ?
                    selectedShowTime.get("showtimeKey").toString() : "demo-key";

            List<String> availableSeats = movieServices.getAvailableSeats(showtimeKey);
            if (availableSeats == null) {
                availableSeats = Collections.emptyList();
            }

            // Store data in session
            String ticketCount = parameters.getOrDefault("count", "1");
            sessionParams.put("selectedShowTime", time);
            sessionParams.put("availableSeats", new Gson().toJson(availableSeats));
            sessionParams.put("maxSeats", ticketCount);
            sessionParams.put("showtimeKey", showtimeKey);
            sessionParameters.put(sessionId, sessionParams);

            // Prepare response
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("showSeatSelection", true);
            responseData.put("availableSeats", availableSeats);
            responseData.put("maxSeats", ticketCount);

            return ResponseEntity.ok(new ChatbotResponse(List.of(
                    new Gson().toJson(responseData),
                    "You've selected showtime: " + time,
                    "Please select " + ticketCount + " seat(s) from the available options."
            )));

        } catch (NumberFormatException e) {
            return ResponseEntity.ok(new ChatbotResponse(List.of(
                    "Please enter a valid number for the showtime selection."
            )));
        } catch (JsonSyntaxException e) {
            return ResponseEntity.ok(new ChatbotResponse(List.of(
                    "Error processing showtime data. Please start over."
            )));
        } catch (Exception e) {
            System.err.println("Error in handleShowtimeSelection: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(new ChatbotResponse(List.of(
                    "An unexpected error occurred. Please try again later."
            )));
        }
    }

    private ResponseEntity<ChatbotResponse> handleSeatSelection(Map<String, String> parameters,
                                                                String sessionId,
                                                                String selectedSeatsInput) {
        // Declare variables outside try so they are accessible in catch
        String showtimeId = null;
        List<String> selectedSeats = new ArrayList<>();
        Booking booking = null;

        try {
            // 1. Validate seat selection
            if (selectedSeatsInput == null || selectedSeatsInput.isEmpty()) {
                return ResponseEntity.ok(new ChatbotResponse(List.of("Please select seats")));
            }

            showtimeId = parameters.get("showtimeKey");
            String userId = parameters.getOrDefault("userId", "guest");

            // 2. Process selected seats
            selectedSeats = Arrays.stream(selectedSeatsInput.split(","))
                    .map(String::trim)
                    .filter(seat -> !seat.isEmpty())
                    .collect(Collectors.toList());

            // 3. Check seat availability
            List<String> availableSeats = movieServices.getAvailableSeats(showtimeId);
            List<String> invalidSeats = selectedSeats.stream()
                    .filter(seat -> !availableSeats.contains(seat))
                    .collect(Collectors.toList());

            if (!invalidSeats.isEmpty()) {
                return ResponseEntity.ok(new ChatbotResponse(List.of(
                        "Seats not available: " + String.join(", ", invalidSeats),
                        "Please choose different seats"
                )));
            }

            // 4. Create temporary booking
            booking = new Booking();
            booking.setShowtimeId(showtimeId);
            booking.setSeatNumbers(selectedSeats);
            booking.setStatus("PENDING");
            booking.setUserId(userId);
            booking.setMovieTitle(parameters.get("movie"));
            booking.setCinemaName(parameters.get("theatre"));
            booking.setTotalAmount((double) (selectedSeats.size() * 150));
            booking = bookingRepository.save(booking);

            // 5. Reserve seats
            boolean reserved = movieServices.bookSeats(showtimeId, selectedSeats, userId);
            if (!reserved) {
                bookingRepository.delete(booking);
                return ResponseEntity.ok(new ChatbotResponse(List.of(
                        "Seats were just booked by another user",
                        "Available seats: " + String.join(", ", availableSeats)
                )));
            }

            // 6. Create payment link
            try {
                String userEmail = parameters.getOrDefault("userEmail", "jehieljehiel666@gmail.com");
                String paymentLink = cashfreePaymentService.createPaymentLinkAndSendTicket(
                        booking.getId(),
                        booking.getTotalAmount(),
                        "Ticket for " + parameters.get("movie"),
                        userEmail
                );

                // Send ticket confirmation email (regardless of payment)
                sendTicketConfirmationEmail(booking, userEmail, paymentLink);

                parameters.put("bookingId", booking.getId());
                sessionParameters.put(sessionId, parameters);

                String htmlMessage = "<div>" +
                        "<p>💳 Please complete your payment:</p>" +
                        "<p><a href=\"" + paymentLink + "\" target=\"_blank\">Click here to pay</a></p>" +
                        "<p>Amount: ₹" + booking.getTotalAmount() + "</p>" +
                        "</div>";

                return ResponseEntity.ok(new ChatbotResponse(List.of(htmlMessage)));
            } catch (Exception e) {
                // Error handling and cleanup
                if (showtimeId != null && !selectedSeats.isEmpty()) {
                    movieServices.releaseSeats(showtimeId, selectedSeats);
                }
                if (booking != null) {
                    bookingRepository.delete(booking);
                }

                return ResponseEntity.ok(new ChatbotResponse(List.of(
                        "⚠️ Payment processing failed",
                        "Error: " + e.getMessage(),
                        "Please try again or contact support"
                )));
            }
        } catch (Exception e) {
            logger.error("Error in handleSeatSelection", e);
            // Cleanup in case of any error
            if (showtimeId != null && !selectedSeats.isEmpty()) {
                movieServices.releaseSeats(showtimeId, selectedSeats);
            }
            if (booking != null) {
                bookingRepository.delete(booking);
            }

            return ResponseEntity.ok(new ChatbotResponse(List.of(
                    "An unexpected error occurred during seat selection",
                    "Please try again later"
            )));
        }
    }

    private ResponseEntity<ChatbotResponse> handleMovieDetailsIntent(Map<String, String> parameters, String sessionId) {
        try {
            String movieName = parameters.getOrDefault("movie", "");
            String movie_year = parameters.getOrDefault("movie-year", "");
            System.out.println("Movie Name : " + movieName + " Year : " + movie_year);
            if (movieName.isEmpty()) {
                return buildClarificationResponse("Which movie would you like to know about?");
            }

            Map<String, Object> movieDetails = movieServices.getMovieDetailsWithReviews(movieName, movie_year);
            if (movieDetails.isEmpty()) {
                return buildNotFoundResponse(movieName, "");
            }

            return buildMovieDetailsResponse(movieName, movie_year, movieDetails);
        } catch (Exception e) {
            logger.error("Error handling movie details intent", e);
            return buildErrorResponse();
        }
    }

    private ResponseEntity<ChatbotResponse> buildMovieDetailsResponse(String movieName, String movie_year, Map<String, Object> movieDetails) {
        StringBuilder response = new StringBuilder();
        response.append("<div style='max-width:600px;'>");
        response.append("<h3>").append(movieDetails.getOrDefault("title", movieName)).append("</h3>");

        if (movieDetails.containsKey("poster") && !"N/A".equalsIgnoreCase(movieDetails.get("poster").toString())) {
            response.append("<div style='margin-top:15px; text-align:center;'>")
                    .append("<img src='").append(movieDetails.get("poster"))
                    .append("' style='max-width:100%; max-height:300px;' alt='Movie poster'>")
                    .append("</div>");
        }

        // Basic info
        response.append("<div style='margin-bottom:15px;'>");
        if (movieDetails.containsKey("year")) {
            response.append("<div><b>Year:</b> ").append(movieDetails.get("year")).append("</div>");
        }
        if (movieDetails.containsKey("certification")) {
            response.append("<div><b>Age Rating:</b> ").append(movieDetails.get("certification")).append("</div>");
        }
        if (movieDetails.containsKey("rated") && !"N/A".equalsIgnoreCase(movieDetails.get("rated").toString())) {
            response.append("<div><b>Rating:</b> ").append(movieDetails.get("rated")).append("</div>");
        }
        if (movieDetails.containsKey("runtime")) {
            response.append("<div><b>Duration:</b> ").append(movieDetails.get("runtime")).append("</div>");
        }
        if (movieDetails.containsKey("genre")) {
            response.append("<div><b>Genre:</b> ").append(movieDetails.get("genre")).append("</div>");
        }
        response.append("</div>");

        // Plot
        if (movieDetails.containsKey("plot")) {
            response.append("<div style='margin-bottom:15px;'><b>Plot:</b><br>")
                    .append(movieDetails.get("plot")).append("</div>");
        }

        // Ratings
        if (movieDetails.containsKey("ratings")) {
            List<Map<String, String>> ratings = (List<Map<String, String>>) movieDetails.get("ratings");
            if (!ratings.isEmpty()) {
                response.append("<div style='margin-bottom:15px;'><b>Ratings:</b><ul>");
                ratings.forEach(rating -> {
                    response.append("<li>")
                            .append(rating.get("Source")).append(": ")
                            .append(rating.get("Value"))
                            .append("</li>");
                });
                response.append("</ul></div>");
            }
        }

        // Cast
        if (movieDetails.containsKey("actors")) {
            response.append("<div><b>Cast:</b> ")
                    .append(movieDetails.get("actors")).append("</div>");
        }

        if (movieDetails.containsKey("trailer")) {
            response.append("<div><b>Trailer:</b> <a href='")
                    .append(movieDetails.get("trailer"))
                    .append("' target='_blank'>Watch Trailer</a></div>");
        }

        // Now Showing
        boolean isShowing = tmdbService.isMovieNowPlaying(movieName);
        response.append("<div><b>Now Showing:</b> ")
                .append(isShowing ? "✅ Yes" : "❌ No")
                .append("</div>");

        response.append("</div>");
        return ResponseEntity.ok(new ChatbotResponse(List.of(response.toString())));
    }

    private ResponseEntity<ChatbotResponse> buildClarificationResponse(String message) {
        return ResponseEntity.ok(new ChatbotResponse(List.of(message)));
    }

    private ResponseEntity<ChatbotResponse> buildNotFoundResponse(String movieName, String location) {
        return ResponseEntity.ok(new ChatbotResponse(List.of(
                String.format("I couldn't find \"%s\" in %s. Please check the movie name and try again.", movieName, location)
        )));
    }

    private ResponseEntity<ChatbotResponse> buildErrorResponse() {
        return ResponseEntity.ok(new ChatbotResponse(List.of(
                "Sorry, I encountered an error processing your request. Please try again later."
        )));
    }


    private ResponseEntity<ChatbotResponse> handleFallbackIntent() {
        String fulfillmentText = "I'm sorry, I didn't understand that. Can you please rephrase?";
        return ResponseEntity.ok(new ChatbotResponse(List.of(fulfillmentText)));
    }

    private ResponseEntity<ChatbotResponse> handleUnknownIntent() {
        String fulfillmentText = "I'm not sure how to handle that request. Please try again.";
        return ResponseEntity.ok(new ChatbotResponse(List.of(fulfillmentText)));
    }

    private String FormDate(String date) {
        try {
            // Extract a valid date from the input using regex
            Pattern pattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
            Matcher matcher = pattern.matcher(date);

            if (matcher.find()) {
                String extractedDate = matcher.group(); // e.g., "2025-05-24"
                LocalDate localDate = LocalDate.parse(extractedDate, DateTimeFormatter.ISO_DATE);
                String formattedDate = localDate.format(DateTimeFormatter.ISO_DATE);
                System.out.println("Formatted Date : " + formattedDate);
                return formattedDate;
            } else {
                System.err.println("No valid date found in input: " + date);
                return "INVALID_DATE";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "INVALID_DATE";
        }
    }

    private void clearSessionParameters(String sessionId) {
        sessionParameters.remove(sessionId);
    }

    private void sendTicketConfirmationEmail(Booking booking, String userEmail, String paymentLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(userEmail);
            helper.setSubject("Your Ticket Booking Confirmation - Pending Payment");

            // Create HTML email content
            String htmlContent = buildConfirmationEmailContent(booking, paymentLink);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Ticket confirmation email sent to: " + userEmail);
        } catch (MessagingException e) {
            logger.error("Failed to send confirmation email", e);
        }
    }

    private String buildConfirmationEmailContent(Booking booking, String paymentLink) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "  body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                "  .container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                "  .header { background-color: #f8f9fa; padding: 15px; text-align: center; }" +
                "  .content { padding: 20px; }" +
                "  .button { background-color: #007bff; color: white; padding: 10px 15px; text-decoration: none; border-radius: 5px; display: inline-block; }" +
                "  .footer { margin-top: 20px; font-size: 12px; color: #777; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "  <div class='header'>" +
                "    <h2>Your Ticket Booking Confirmation</h2>" +
                "  </div>" +
                "  <div class='content'>" +
                "    <p>Hello,</p>" +
                "    <p>Thank you for booking with us! Here are your booking details:</p>" +
                "    <p><strong>Movie:</strong> " + booking.getMovieTitle() + "</p>" +
                "    <p><strong>Theater:</strong> " + booking.getCinemaName() + "</p>" +
                "    <p><strong>Seats:</strong> " + String.join(", ", booking.getSeatNumbers()) + "</p>" +
                "    <p><strong>Total Amount:</strong> ₹" + booking.getTotalAmount() + "</p>" +
                "    <p><strong>Status:</strong> Pending Payment</p>" +
                "    <p style='margin-top: 20px;'>" +
                "      <a href='" + paymentLink + "' class='button'>Complete Payment Now</a>" +
                "    </p>" +
                "    <p>Please complete your payment to secure your seats. Your booking will be confirmed once payment is received.</p>" +
                "    <p>If you didn't make this booking, please ignore this email.</p>" +
                "  </div>" +
                "  <div class='footer'>" +
                "    <p>Thank you for choosing our service!</p>" +
                "  </div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
}