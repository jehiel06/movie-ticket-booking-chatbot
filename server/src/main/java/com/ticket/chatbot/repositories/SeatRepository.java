package com.ticket.chatbot.repositories;

import com.ticket.chatbot.model.Seat;
import com.ticket.chatbot.model.SeatStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends MongoRepository<Seat, String>, SeatRepositoryCustom {

    // Find available seats for a showtime
    List<Seat> findByShowtimeIdAndStatus(String showtimeId, SeatStatus status);

    // Find unavailable seats (e.g., RESERVED or BOOKED)
    @Query("{'showtimeId': ?0, 'number': {$in: ?1}, 'status': {$ne: 'AVAILABLE'}}")
    List<Seat> findByShowtimeIdAndNumberInAndStatusNot(String showtimeId, List<String> seatNumbers, SeatStatus status);
}
