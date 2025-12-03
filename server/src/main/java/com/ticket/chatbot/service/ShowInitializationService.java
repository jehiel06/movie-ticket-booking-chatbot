package com.ticket.chatbot.service;

import com.ticket.chatbot.model.Seat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ShowInitializationService {

    @Autowired
    private MongoTemplate mongoTemplate;

    public void initializeShow(String showtimeId, int rows, int seatsPerRow) {
        List<Seat> seats = new ArrayList<>();

        for (char row = 'A'; row < 'A' + rows; row++) {
            for (int num = 1; num <= seatsPerRow; num++) {
                Seat seat = new Seat();
                seat.setShowtimeId(showtimeId);
                seat.setSeatNumber(row + String.valueOf(num));
                seat.setStatus("AVAILABLE");
                seats.add(seat);
            }
        }

        mongoTemplate.insertAll(seats);
    }
}