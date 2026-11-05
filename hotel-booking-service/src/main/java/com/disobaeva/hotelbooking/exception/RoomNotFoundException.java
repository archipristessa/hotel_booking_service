package com.disobaeva.hotelbooking.exception;

/** Номер не найден по идентификатору. */
public class RoomNotFoundException extends EntityNotFoundException {

    public RoomNotFoundException(Long roomId) {
        super("Номер с идентификатором " + roomId + " не найден");
    }
}
