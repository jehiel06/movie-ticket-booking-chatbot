package com.ticket.chatbot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TmdbService {

    @Value("${tmdb.api.key}")
    private String tmdbApiKey;

    private final RestTemplate restTemplate;
    private static final Logger logger = LoggerFactory.getLogger(TmdbService.class);

    public TmdbService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean isMovieNowPlaying(String movieTitle) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://api.themoviedb.org/3/movie/now_playing")
                    .queryParam("api_key", tmdbApiKey)
                    .queryParam("language", "en-US")
                    .queryParam("region", "IN")
                    .toUriString();

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody().get("results");
            if (results == null) return false;

            return results.stream().anyMatch(movie ->
                    movie.get("title").toString().equalsIgnoreCase(movieTitle) ||
                            movie.get("title").toString().toLowerCase().contains(movieTitle.toLowerCase())
            );
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Optional<String> getTrailerUrl(String movieTitle, String movieYear) {
        try {
            // First try exact search with year
            String searchUrl = UriComponentsBuilder
                    .fromHttpUrl("https://api.themoviedb.org/3/search/movie")
                    .queryParam("api_key", tmdbApiKey)
                    .queryParam("query", movieTitle)
                    .queryParam("year", movieYear)
                    .queryParam("include_adult", "false")
                    .toUriString();

            ResponseEntity<Map> response = restTemplate.getForEntity(searchUrl, Map.class);
            List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody().get("results");

            if (results == null || results.isEmpty()) {
                searchUrl = UriComponentsBuilder
                        .fromHttpUrl("https://api.themoviedb.org/3/search/movie")
                        .queryParam("api_key", tmdbApiKey)
                        .queryParam("query", movieTitle)
                        .queryParam("include_adult", "false")
                        .toUriString();
            }
            if (results == null || results.isEmpty()) return Optional.empty();

            Optional<Map<String, Object>> bestMatch = results.stream()
                    .max(Comparator.comparingDouble(movie -> {
                    String title = movie.get("title").toString().toLowerCase();
                    return stringSimilarity(movieTitle.toLowerCase(), title);
                    }));

            if (bestMatch.isPresent() &&
                stringSimilarity(movieTitle.toLowerCase(),
                bestMatch.get().get("title").toString().toLowerCase()) > 0.7) {
                String movieId = String.valueOf(bestMatch.get().get("id"));
                return fetchTrailerFromMovieId(movieId);
            }

            return Optional.empty();

        } catch(Exception e) {
            logger.error("Error fetching trailer for movie: {}", movieTitle, e);
            return Optional.empty();
        }
    }

    private double stringSimilarity(String s1, String s2) {
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) return 1.0;

        int editDistance = computeLevenshteinDistance(s1, s2);
        return 1.0 - (double) editDistance / maxLength;
    }

    private int computeLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                            Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                            dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1)
                    );
                }
            }
        }
        return dp[s1.length()][s2.length()];
    }

    private Optional<String> fetchTrailerFromMovieId(String movieId) {
        try {
            String videosUrl = UriComponentsBuilder
                    .fromHttpUrl("https://api.themoviedb.org/3/movie/" + movieId + "/videos")
                    .queryParam("api_key", tmdbApiKey)
                    .toUriString();

            ResponseEntity<Map> videoResponse = restTemplate.getForEntity(videosUrl, Map.class);
            List<Map<String, Object>> videos = (List<Map<String, Object>>) videoResponse.getBody().get("results");

            if (videos != null) {
                return videos.stream()
                        .filter(video -> "Trailer".equalsIgnoreCase((String) video.get("type")) &&
                                "YouTube".equalsIgnoreCase((String) video.get("site")))
                        .map(video -> "https://www.youtube.com/watch?v=" + video.get("key"))
                        .findFirst();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public Map<String, Object> getMovieDetailsWithExtras(String movieTitle, String releaseYear) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Improved search with better matching
            String searchUrl = UriComponentsBuilder
                    .fromHttpUrl("https://api.themoviedb.org/3/search/movie")
                    .queryParam("api_key", tmdbApiKey)
                    .queryParam("query", movieTitle)
                    .queryParam("year", releaseYear)
                    .queryParam("include_adult", "false")
                    .toUriString();

            ResponseEntity<Map> searchResponse = restTemplate.getForEntity(searchUrl, Map.class);
            List<Map<String, Object>> searchResults = (List<Map<String, Object>>) searchResponse.getBody().get("results");

            if (searchResults == null || searchResults.isEmpty()) {
                // Try again without year if no results
                searchUrl = UriComponentsBuilder
                        .fromHttpUrl("https://api.themoviedb.org/3/search/movie")
                        .queryParam("api_key", tmdbApiKey)
                        .queryParam("query", movieTitle)
                        .queryParam("include_adult", "false")
                        .toUriString();

                searchResponse = restTemplate.getForEntity(searchUrl, Map.class);
                searchResults = (List<Map<String, Object>>) searchResponse.getBody().get("results");

                if (searchResults == null || searchResults.isEmpty()) {
                    return result;
                }
            }

            // Find best match using fuzzy matching
            Optional<Map<String, Object>> matchingMovie = searchResults.stream()
                    .max(Comparator.comparingDouble(movie -> {
                        String title = movie.get("title").toString().toLowerCase();
                        return stringSimilarity(movieTitle.toLowerCase(), title);
                    }));

            if (matchingMovie.isEmpty() ||
                    stringSimilarity(movieTitle.toLowerCase(),
                            matchingMovie.get().get("title").toString().toLowerCase()) < 0.6) {
                return result;
            }

            Map<String, Object> movie = matchingMovie.get();
            String movieId = String.valueOf(movie.get("id"));

            // Basic info
            result.put("title", movie.get("title"));
            result.put("plot", movie.get("overview"));
            result.put("poster", movie.get("poster_path") != null
                    ? "https://image.tmdb.org/t/p/w500" + movie.get("poster_path")
                    : "N/A");
            result.put("year", movie.get("release_date") != null
                    ? movie.get("release_date").toString().split("-")[0]
                    : "N/A");

            // Get full details
            String detailUrl = UriComponentsBuilder
                    .fromHttpUrl("https://api.themoviedb.org/3/movie/" + movieId)
                    .queryParam("api_key", tmdbApiKey)
                    .toUriString();

            ResponseEntity<Map> detailResponse = restTemplate.getForEntity(detailUrl, Map.class);
            Map<String, Object> detailBody = detailResponse.getBody();

            // Certification
            String certification = getMovieCertification(movieId);
            if (certification != null) {
                result.put("certification", certification);
            }

            if (detailBody != null) {
                if (detailBody.get("runtime") != null) {
                    result.put("runtime", detailBody.get("runtime") + " min");
                }
                if (detailBody.get("genres") instanceof List genres && !genres.isEmpty()) {
                    result.put("genre", (String) genres.stream()
                            .map(g -> ((Map<String, Object>) g).get("name").toString())
                            .collect(Collectors.joining(", ")));
                }
                if (detailBody.get("vote_average") != null) {
                    result.put("rated", detailBody.get("vote_average").toString() + "/10");
                }
            }

            // Cast (unchanged)
            String creditsUrl = UriComponentsBuilder
                    .fromHttpUrl("https://api.themoviedb.org/3/movie/" + movieId + "/credits")
                    .queryParam("api_key", tmdbApiKey)
                    .toUriString();

            ResponseEntity<Map> creditsResponse = restTemplate.getForEntity(creditsUrl, Map.class);
            List<Map<String, Object>> cast = (List<Map<String, Object>>) creditsResponse.getBody().get("cast");

            if (cast != null && !cast.isEmpty()) {
                String actorList = cast.stream()
                        .limit(5)
                        .map(c -> c.get("name").toString())
                        .collect(Collectors.joining(", "));
                result.put("actors", actorList);
            }

            // Trailer - use the improved getTrailerUrl method
            getTrailerUrl(movieTitle, releaseYear).ifPresent(trailerUrl -> result.put("trailer", trailerUrl));

            // Improved reviews fetching - sorted by date and limited
            String reviewsUrl = UriComponentsBuilder
                    .fromHttpUrl("https://api.themoviedb.org/3/movie/" + movieId + "/reviews")
                    .queryParam("api_key", tmdbApiKey)
                    .queryParam("sort_by", "created_at.desc") // Sort by newest first
                    .toUriString();

            ResponseEntity<Map> reviewResponse = restTemplate.getForEntity(reviewsUrl, Map.class);
            List<Map<String, Object>> reviews = (List<Map<String, Object>>) reviewResponse.getBody().get("results");

            if (reviews != null && !reviews.isEmpty()) {
                List<Map<String, Object>> simplifiedReviews = reviews.stream()
                        .sorted(Comparator.comparing(
                                r -> r.get("created_at").toString(),
                                Comparator.reverseOrder()
                        ))
                        .limit(3) // Only get 3 most recent
                        .map(r -> Map.of(
                                "author", r.get("author"),
                                "content", r.get("content"),
                                "date", r.get("created_at")
                        ))
                        .collect(Collectors.toList());
                result.put("reviews", simplifiedReviews);
            }

        } catch (Exception e) {
            logger.error("Error getting movie details for: {}", movieTitle, e);
        }

        return result;
    }

    private String getMovieCertification(String movieId) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://api.themoviedb.org/3/movie/" + movieId + "/release_dates")
                    .queryParam("api_key", tmdbApiKey)
                    .toUriString();

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody().get("results");

            if (results != null) {
                // Try to find India certification first
                for (Map<String, Object> countryData : results) {
                    if ("IN".equals(countryData.get("iso_3166_1"))) {
                        List<Map<String, Object>> releaseDates = (List<Map<String, Object>>) countryData.get("release_dates");
                        if (releaseDates != null) {
                            for (Map<String, Object> releaseDate : releaseDates) {
                                if (releaseDate.get("certification") != null &&
                                        !releaseDate.get("certification").toString().isEmpty()) {
                                    return releaseDate.get("certification").toString();
                                }
                            }
                        }
                    }
                }

                // Fallback to US certification
                for (Map<String, Object> countryData : results) {
                    if ("US".equals(countryData.get("iso_3166_1"))) {
                        List<Map<String, Object>> releaseDates = (List<Map<String, Object>>) countryData.get("release_dates");
                        if (releaseDates != null) {
                            for (Map<String, Object> releaseDate : releaseDates) {
                                if (releaseDate.get("certification") != null &&
                                        !releaseDate.get("certification").toString().isEmpty()) {
                                    return releaseDate.get("certification").toString();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}