package com.disobaeva.hotelbooking.exception;

/** Операция невозможна в текущем состоянии бронирования (например, подтвердить отменённое). */
public class InvalidBookingStateException extends BusinessRuleException {

    public InvalidBookingStateException(String message) {
        super(message);
    }
}
