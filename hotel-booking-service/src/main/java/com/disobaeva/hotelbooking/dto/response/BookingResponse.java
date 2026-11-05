package com.disobaeva.hotelbooking.dto.response;

import com.disobaeva.hotelbooking.model.entity.Booking;
import com.disobaeva.hotelbooking.model.entity.BookingStatus;
import com.disobaeva.hotelbooking.model.entity.Hotel;
import com.disobaeva.hotelbooking.model.entity.Room;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Ответ по бронированию: все нужные поля «развёрнуты» в плоскую структуру,
 * поэтому в JSON нет циклов Booking → Room → Hotel → rooms → ...
 */
public record BookingResponse(
        Long bookingId,
        Long clientId,
        String clientName,
        Long roomId,
        String roomNumber,
        Long hotelId,
        String hotelName,
        LocalDate checkIn,
        LocalDate checkOut,
        int guestCount,
        long nights,
        BigDecimal pricePerNight,
        BigDecimal total,
        BookingStatus status
) {
    public static BookingResponse from(Booking booking) {
        Room room = booking.getRoom();
        Hotel hotel = room.getHotel();
        return new BookingResponse(
                booking.getId(),
                booking.getClient().getId(),
                booking.getClient().getFullName(),
                room.getId(),
                room.getNumber(),
                hotel.getId(),
                hotel.getName(),
                booking.getCheckIn(),
                booking.getCheckOut(),
                booking.getGuestCount(),
                booking.getNights(),
                booking.getPricePerNight(),
                booking.calculateTotal(),
                booking.getStatus()
        );
    }
}
