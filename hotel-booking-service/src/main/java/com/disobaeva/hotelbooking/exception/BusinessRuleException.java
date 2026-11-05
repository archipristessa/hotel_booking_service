package com.disobaeva.hotelbooking.exception;

/**
 * Ситуация «нарушено бизнес-правило» или «операция невозможна в текущем состоянии».
 * Конкретные правила имеют свои подклассы (см. пакет).
 */
public class BusinessRuleException extends DomainException {

    public static final String ERROR_CODE = "BUSINESS_RULE_VIOLATION";

    public BusinessRuleException(String message) {
        super(ERROR_CODE, message);
    }
}
