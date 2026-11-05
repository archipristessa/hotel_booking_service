package com.disobaeva.hotelbooking.exception;

/** Бронирование не найдено по идентификатору. */
public class BookingNotFoundException extends EntityNotFoundException {

    public BookingNotFoundException(Long bookingId) {
        super("Бронирование с идентификатором " + bookingId + " не найдено");
    }
}
