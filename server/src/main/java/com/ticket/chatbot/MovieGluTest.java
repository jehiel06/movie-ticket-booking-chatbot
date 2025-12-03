package com.ticket.chatbot;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public class MovieGluTest {

    public static void main(String[] args) {
        String filmId = "384446";
        String date = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String url = "https://api-gate2.movieglu.com/filmShowTimes/?film_id=" + filmId + "&date=" + date + "&n=2";

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));

        // === Set MovieGlu Required Headers ===
        headers.add("client", "STUD_376");
        headers.add("x-api-key", "nryuczQ2ZgRf01TAn0Tu2XbZ3tpodzQ4WAIAGo4h");
        headers.add("authorization", "Basic U1RVRF8zNzY6ZkZUTlppdE5kWGp4"); // or generate below
        headers.add("territory", "IN");
        headers.add("api-version", "v201");
        headers.add("device-datetime", ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")));
        headers.add("geolocation", "13.0827;80.2707"); // Chennai
        headers.add("user-agent", "TicketBot/1.0");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            System.out.println("Status Code: " + response.getStatusCode());
            System.out.println("Response Body:");
            System.out.println(response.getBody());
        } catch (Exception e) {
            System.err.println("Request failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
