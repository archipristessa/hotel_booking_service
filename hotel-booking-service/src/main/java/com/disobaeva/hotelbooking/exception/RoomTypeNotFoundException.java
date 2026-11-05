package com.disobaeva.hotelbooking.exception;

/** Тип номера не найден по идентификатору. */
public class RoomTypeNotFoundException extends EntityNotFoundException {

    public RoomTypeNotFoundException(Long roomTypeId) {
        super("Тип номера с идентификатором " + roomTypeId + " не найден");
    }
}
