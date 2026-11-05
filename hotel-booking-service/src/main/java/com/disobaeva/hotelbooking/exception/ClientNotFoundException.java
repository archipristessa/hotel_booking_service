package com.disobaeva.hotelbooking.exception;

/** Клиент не найден по идентификатору. */
public class ClientNotFoundException extends EntityNotFoundException {

    public ClientNotFoundException(Long clientId) {
        super("Клиент с идентификатором " + clientId + " не найден");
    }
}
