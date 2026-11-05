package com.disobaeva.hotelbooking.exception;

/** Количество гостей превышает вместимость номера (или не положительно). */
public class RoomCapacityExceededException extends BusinessRuleException {

    public RoomCapacityExceededException(int capacity, int guestCount) {
        super("Номер вмещает " + capacity + " гостей, а запрошено " + guestCount);
    }

    public RoomCapacityExceededException(String message) {
        super(message);
    }
}
