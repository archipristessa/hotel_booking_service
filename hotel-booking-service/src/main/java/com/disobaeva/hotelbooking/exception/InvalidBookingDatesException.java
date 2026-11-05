package com.disobaeva.hotelbooking.exception;

/**
 * Некорректный период проживания: дата выезда не позже даты заезда,
 * либо дата заезда находится в прошлом.
 */
public class InvalidBookingDatesException extends BusinessRuleException {

    public InvalidBookingDatesException(String message) {
        super(message);
    }
}
