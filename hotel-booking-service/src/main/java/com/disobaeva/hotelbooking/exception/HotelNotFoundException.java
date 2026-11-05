package com.disobaeva.hotelbooking.exception;

/** Отель не найден по идентификатору. */
public class HotelNotFoundException extends EntityNotFoundException {

    public HotelNotFoundException(Long hotelId) {
        super("Отель с идентификатором " + hotelId + " не найден");
    }
}
