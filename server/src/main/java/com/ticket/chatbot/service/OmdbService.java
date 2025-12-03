/*
package com.ticket.chatbot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OmdbService {

    private static final Logger logger = LoggerFactory.getLogger(OmdbService.class);

    @Value("${omdb.api.url}")
    private String omdbApiUrl;

    @Value("${omdb.api.key}")
    private String omdbApiKey;

    private final RestTemplate restTemplate;

    public OmdbService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<String, Object> getMovieDetails(String movieTitle) {
        try {
            String encodedTitle = URLEncoder.encode(movieTitle, StandardCharsets.UTF_8.toString());
            String url = UriComponentsBuilder.fromHttpUrl(omdbApiUrl)
                    .queryParam("t", encodedTitle)
                    .queryParam("apikey", omdbApiKey)
                    .queryParam("plot", "full")  // Get full plot
                    .queryParam("r", "json")     // JSON response
                    .toUriString();

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();

                // Check if movie was found
                if ("False".equals(responseBody.get("Response"))) {
                    logger.warn("Movie not found: {}", movieTitle);
                    return Collections.emptyMap();
                }

                return processOmdbResponse(responseBody);
            }
        } catch (Exception e) {
            logger.error("Error fetching movie details from OMDB", e);
        }
        return Collections.emptyMap();
    }

    private Map<String, Object> processOmdbResponse(Map<String, Object> omdbData) {
        Map<String, Object> processedData = new HashMap<>();

        // Basic info
        processedData.put("title", omdbData.get("Title"));
        processedData.put("year", omdbData.get("Year"));
        processedData.put("rated", omdbData.get("Rated"));
        processedData.put("released", omdbData.get("Released"));
        processedData.put("runtime", omdbData.get("Runtime"));
        processedData.put("genre", omdbData.get("Genre"));
        processedData.put("director", omdbData.get("Director"));
        processedData.put("writer", omdbData.get("Writer"));
        processedData.put("actors", omdbData.get("Actors"));
        processedData.put("plot", omdbData.get("Plot"));
        processedData.put("language", omdbData.get("Language"));
        processedData.put("country", omdbData.get("Country"));
        processedData.put("poster", omdbData.get("Poster"));

        // Ratings
        if (omdbData.containsKey("Ratings")) {
            List<Map<String, String>> ratings = (List<Map<String, String>>) omdbData.get("Ratings");
            processedData.put("ratings", ratings);

            // Extract specific ratings
            ratings.forEach(rating -> {
                String source = rating.get("Source");
                if ("Internet Movie Database".equals(source)) {
                    processedData.put("imdb_rating", rating.get("Value"));
                } else if ("Rotten Tomatoes".equals(source)) {
                    processedData.put("rotten_tomatoes", rating.get("Value"));
                } else if ("Metacritic".equals(source)) {
                    processedData.put("metacritic", rating.get("Value"));
                }
            });
        }

        // Additional info
        processedData.put("metascore", omdbData.get("Metascore"));
        processedData.put("imdbRating", omdbData.get("imdbRating"));
        processedData.put("imdbVotes", omdbData.get("imdbVotes"));
        processedData.put("imdbID", omdbData.get("imdbID"));
        processedData.put("type", omdbData.get("Type"));

        return processedData;
    }
}*/
