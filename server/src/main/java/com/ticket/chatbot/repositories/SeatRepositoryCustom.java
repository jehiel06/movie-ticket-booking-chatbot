package com.ticket.chatbot.repositories;

import java.util.List;

public interface SeatRepositoryCustom {
    void reserveSeats(String showtimeId, List<String> seatNumbers, String bookingId);
    void confirmSeats(String showtimeId, List<String> seatNumbers);
}
