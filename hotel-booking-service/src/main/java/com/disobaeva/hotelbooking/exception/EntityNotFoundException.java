package com.disobaeva.hotelbooking.exception;

/**
 * Ситуация «запрошенный объект не существует».
 * Конкретные сущности (отель, номер, бронирование...) имеют свои подклассы.
 */
public class EntityNotFoundException extends DomainException {

    public static final String ERROR_CODE = "ENTITY_NOT_FOUND";

    public EntityNotFoundException(String message) {
        super(ERROR_CODE, message);
    }
}
