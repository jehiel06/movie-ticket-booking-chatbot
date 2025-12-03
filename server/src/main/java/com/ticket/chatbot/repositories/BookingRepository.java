package com.ticket.chatbot.repositories;

import com.ticket.chatbot.model.Booking;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface BookingRepository extends MongoRepository<Booking, String> {
    // Replace with methods that match your actual model fields
    List<Booking> findByShowtimeId(String showtimeId);
    List<Booking> findByUserId(String userId);
}
