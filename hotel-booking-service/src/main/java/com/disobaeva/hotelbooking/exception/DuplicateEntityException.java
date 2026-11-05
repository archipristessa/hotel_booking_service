package com.disobaeva.hotelbooking.exception;

/**
 * Нарушение уникальности: повторяющийся email клиента, имя типа номера
 * или номер комнаты в пределах одного отеля.
 */
public class DuplicateEntityException extends BusinessRuleException {

    public DuplicateEntityException(String message) {
        super(message);
    }
}
