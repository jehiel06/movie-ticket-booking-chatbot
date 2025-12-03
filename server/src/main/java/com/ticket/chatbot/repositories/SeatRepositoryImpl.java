package com.ticket.chatbot.repositories;

import com.ticket.chatbot.model.SeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SeatRepositoryImpl implements SeatRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    public void reserveSeats(String showtimeId, List<String> seatNumbers, String bookingId) {
        Query query = new Query(Criteria.where("showtimeId").is(showtimeId)
                .and("number").in(seatNumbers)
                .and("status").is(SeatStatus.AVAILABLE));
        Update update = new Update()
                .set("status", SeatStatus.RESERVED)
                .set("bookingId", bookingId);

        mongoTemplate.updateMulti(query, update, "seat");
    }

    public void confirmSeats(String showtimeId, List<String> seatNumbers) {
        Query query = new Query(Criteria.where("showtimeId").is(showtimeId)
                .and("number").in(seatNumbers)
                .and("status").is(SeatStatus.RESERVED));
        Update update = new Update().set("status", SeatStatus.BOOKED);

        mongoTemplate.updateMulti(query, update, "seat");
    }
}
