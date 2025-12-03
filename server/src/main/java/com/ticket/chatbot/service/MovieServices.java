package com.ticket.chatbot.service;

import com.ticket.chatbot.model.Seat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class MovieServices {

    private static final Logger logger = LoggerFactory.getLogger(MovieServices.class);

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private TmdbService tmdbService;


    @Value("${movieglu.api.key}")
    private String movieGluApiKey;

    @Value("${movieglu.api.url}")
    private String movieGluApiUrl;

    @Value("${movieglu.client}")
    private String movieGluClient;

    @Value("${movieglu.authorization}")
    private String movieGluAuthorization;

    @Value("${movieglu.territory}")
    private String movieGluTerritory;

    @Value("${movieglu.api.version}")
    private String movieGluApiVersion;

    private final RestTemplate restTemplate;
    private final Map<String, Set<String>> bookedSeats = new ConcurrentHashMap<>();

    public MovieServices(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private HttpHeaders createMovieGluHeaders(String location) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        headers.add("client", movieGluClient);
        headers.add("x-api-key", movieGluApiKey);
        headers.add("authorization", movieGluAuthorization);
        headers.add("territory", movieGluTerritory);
        headers.add("api-version", movieGluApiVersion);
        headers.add("device-datetime", ZonedDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")));
        headers.add("geolocation", getGeoLocationForCity(location));
        headers.add("user-agent", "TicketBot/1.0");

        return headers;
    }

    public List<Map<String, Object>> getShowtimes(String movieName, String location, String date, String theatre) {
        try {
            System.out.println("Fetching showtimes for movie: " + movieName + ", location: " + location + ", date: " + date);

            List<Map<String, Object>> films = getFilmsNearLocation(location);

            Optional<Map<String, Object>> matchedFilm = films.stream()
                    .filter(film -> film.get("film_name").toString().equalsIgnoreCase(movieName))
                    .findFirst();

            if (matchedFilm.isEmpty()) {
                matchedFilm = films.stream()
                        .filter(film -> film.get("film_name").toString().toLowerCase().contains(movieName.toLowerCase()))
                        .findFirst();
            }

            if (matchedFilm.isEmpty()) {
                System.out.println("No film matched: " + movieName);
                return Collections.emptyList();
            }

            String filmId = matchedFilm.get().get("film_id").toString();
            List<Map<String, Object>> showtimes = getShowtimesFromMovieGlu(filmId, location, date);

            // 🎯 Optional: Filter by theatre if provided
            if (theatre != null && !theatre.isBlank()) {
                showtimes = showtimes.stream()
                        .filter(show -> show.get("cinema").toString().toLowerCase().contains(theatre.toLowerCase()))
                        .toList();
            }

            showtimes.forEach(show -> {
                String showtimeKey = show.get("showtimeKey").toString();
                long availableSeats = mongoTemplate.count(
                        Query.query(Criteria.where("showtimeId").is(showtimeKey)
                                .and("status").is("AVAILABLE")),
                        Seat.class
                );
                show.put("availableSeatsCount", availableSeats);
            });

            return showtimes;

        } catch (Exception e) {
            System.err.println("Error in getShowtimes: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }


    public List<Map<String, Object>> getFilmsNearLocation(String location) {
        try {
            HttpHeaders headers = createMovieGluHeaders(location);

            String url = UriComponentsBuilder.fromHttpUrl(movieGluApiUrl + "/filmsNowShowing/")
                    .queryParam("n", "50")
                    .toUriString();

            HttpEntity<?> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (List<Map<String, Object>>) response.getBody().get("films");
            }
        } catch (Exception e) {
            System.err.println("Error in getFilmsNearLocation: " + e.getMessage());
        }
        return Collections.emptyList();
    }

    private List<Map<String, Object>> getShowtimesFromMovieGlu(String filmId, String location, String date) {
        try {
            HttpHeaders headers = createMovieGluHeaders(location);

            String url = UriComponentsBuilder.fromHttpUrl(movieGluApiUrl + "/filmShowTimes/")
                    .queryParam("film_id", filmId)
                    .queryParam("date", date)
                    .toUriString();

            HttpEntity<?> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> cinemas = (List<Map<String, Object>>) response.getBody().get("cinemas");
                List<Map<String, Object>> showtimes = new ArrayList<>();

                if (cinemas != null) {
                    for (Map<String, Object> cinema : cinemas) {
                        String cinemaName = (String) cinema.get("cinema_name");
                        Map<String, Object> showings = (Map<String, Object>) cinema.get("showings");

                        if (showings != null && showings.containsKey("Standard")) {
                            Map<String, Object> standard = (Map<String, Object>) showings.get("Standard");

                            if (standard != null) {
                                List<Map<String, Object>> times = (List<Map<String, Object>>) standard.get("times");

                                for (Map<String, Object> timeSlot : times) {
                                    String showtimeKey = cinemaName + "_" + timeSlot.get("start_time");
                                    Set<String> booked = bookedSeats.computeIfAbsent(
                                            showtimeKey,
                                            k -> Collections.synchronizedSet(new HashSet<>())
                                    );

                                    List<String> allSeats = generateAllSeats();
                                    List<String> availableSeats = new ArrayList<>(allSeats);
                                    availableSeats.removeAll(booked);

                                    Map<String, Object> showtime = new HashMap<>();
                                    showtime.put("cinema", cinemaName);
                                    showtime.put("time", timeSlot.get("start_time"));
                                    showtime.put("format", timeSlot.get("format"));
                                    showtime.put("availableSeats", availableSeats);
                                    showtime.put("showtimeKey", showtimeKey);
                                    showtimes.add(showtime);
                                }
                            }
                        }
                    }
                }
                return showtimes;
            }
        } catch (Exception e) {
            System.err.println("Error in getShowtimesFromMovieGlu: " + e.getMessage());
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    private void createShowtimeEntry(List<Map<String, Object>> showtimes,
                                     String cinemaName,
                                     Map<String, Object> timeSlot,
                                     String format) {
        try {
            if (timeSlot == null || timeSlot.get("start_time") == null) {
                logger.warn("Invalid time slot data: {}", timeSlot);
                return;
            }

            String showtimeKey = cinemaName + "_" + timeSlot.get("start_time") + "_" + format;

            Map<String, Object> showtime = new HashMap<>();
            showtime.put("cinema", cinemaName);
            showtime.put("time", timeSlot.get("start_time"));
            showtime.put("format", format);
            showtime.put("showtimeKey", showtimeKey);

            // Add additional available information
            if (timeSlot.containsKey("end_time")) {
                showtime.put("end_time", timeSlot.get("end_time"));
            }
            if (timeSlot.containsKey("booking_link")) {
                showtime.put("booking_link", timeSlot.get("booking_link"));
            }

            showtimes.add(showtime);
        } catch (Exception e) {
            logger.error("Error creating showtime entry for cinema: {}", cinemaName, e);
        }
    }

    public Map<String, Object> getMovieDetailsWithReviews(String movieName, String year) {
        try {
            // Fetch basic info + reviews + trailer from TMDB
            Map<String, Object> tmdbDetails = tmdbService.getMovieDetailsWithExtras(movieName, year); // You need to implement this

            // Optionally add showtimes if movie is found in MovieGlu
            List<Map<String, Object>> films = getFilmsNearLocation("");
            Optional<Map<String, Object>> matchedFilm = films.stream()
                    .filter(film -> film.get("film_name").toString().equalsIgnoreCase(movieName))
                    .findFirst();

            if (matchedFilm.isEmpty()) {
                matchedFilm = films.stream()
                        .filter(film -> film.get("film_name").toString().toLowerCase().contains(movieName.toLowerCase()))
                        .findFirst();
            }

            if (matchedFilm.isPresent()) {
                Map<String, Object> movieGluDetails = matchedFilm.get();
                tmdbDetails.put("showtimes", movieGluDetails.get("showtimes"));
                tmdbDetails.put("film_id", movieGluDetails.get("film_id"));
            }

            return tmdbDetails;
        } catch (Exception e) {
            logger.error("Error getting movie details with reviews", e);
            return Collections.emptyMap();
        }
    }

    private String getGeoLocationForCity(String city) {
        if (city == null || city.trim().isEmpty()) return "12.9716;77.5946"; // fallback to Bangalore

        Map<String, String> cityCoordinates = Map.of(
                "chennai", "13.0827;80.2707",
                "mumbai", "19.0760;72.8777",
                "delhi", "28.7041;77.1025",
                "bangalore", "12.9716;77.5946",
                "bengaluru", "12.9716;77.5946",
                "hyderabad", "17.3850;78.4867",
                "kolkata", "22.5726;88.3639",
                "pune", "18.5204;73.8567",
                "coimbatore", "11.0168;76.9558"
        );

        String geo = cityCoordinates.get(city.toLowerCase().trim());
        if (geo == null) {
            logger.warn("Unknown city '{}', falling back to Bangalore", city);
            geo = "12.9716;77.5946"; // default
        }

        return geo;
    }

    private List<String> generateAllSeats() {
        List<String> seats = new ArrayList<>();
        for (char row = 'A'; row <= 'H'; row++) {
            for (int num = 1; num <= 10; num++) {
                seats.add(String.valueOf(row) + num);
            }
        }
        return seats;
    }

    public List<String> getAvailableSeats(String showtimeId) {
        List<Seat> seats = mongoTemplate.find(
                Query.query(Criteria.where("showtimeId").is(showtimeId)
                        .and("status").is("AVAILABLE")),
                Seat.class
        );

        if (seats.isEmpty()) {
            seedSeatsForShowtime(showtimeId);
            return getAvailableSeats(showtimeId); // try again
        }

        return seats.stream().map(Seat::getSeatNumber).collect(Collectors.toList());
    }

    public void seedSeatsForShowtime(String showtimeId) {
        List<Seat> seats = new ArrayList<>();
        for (char row = 'A'; row <= 'H'; row++) {
            for (int num = 1; num <= 10; num++) {
                Seat seat = new Seat();
                seat.setShowtimeId(showtimeId);
                seat.setSeatNumber(String.valueOf(row) + num);
                seat.setStatus("AVAILABLE");
                seats.add(seat);
            }
        }
        mongoTemplate.insertAll(seats);
    }


    // Book seats with userId parameter
    @Transactional
    public synchronized boolean bookSeats(String showtimeId, List<String> seatNumbers, String userId) {
        // Verify all seats are available
        long unavailableCount = mongoTemplate.count(
                Query.query(Criteria.where("showtimeId").is(showtimeId)
                        .and("seatNumber").in(seatNumbers)
                        .and("status").ne("AVAILABLE")),
                Seat.class
        );

        if (unavailableCount > 0) {
            return false;
        }

        // Reserve the seats
        Update update = new Update()
                .set("status", "RESERVED")
                .set("userId", userId);

        mongoTemplate.updateMulti(
                Query.query(Criteria.where("showtimeId").is(showtimeId)
                        .and("seatNumber").in(seatNumbers)),
                update,
                Seat.class
        );

        return true;
    }

    @Transactional
    public void releaseSeats(String showtimeId, List<String> seatNumbers) {
        Update update = new Update().set("status", "AVAILABLE");
        mongoTemplate.updateMulti(
                Query.query(Criteria.where("showtimeId").is(showtimeId)
                        .and("seatNumber").in(seatNumbers)),
                update,
                Seat.class
        );
    }
}
