package com.disobaeva.hotelbooking.exception;

/**
 * Номер уже занят на выбранные даты (пересечение периода бронирования).
 * Это ошибка типа 409 Conflict на Этапе 3.
 */
public class RoomNotAvailableException extends BusinessRuleException {

    public RoomNotAvailableException(Long roomId, Object checkIn, Object checkOut) {
        super("Номер " + roomId + " недоступен на период " + checkIn + " — " + checkOut
                + ": уже есть активное бронирование на пересекающиеся даты");
    }
}
