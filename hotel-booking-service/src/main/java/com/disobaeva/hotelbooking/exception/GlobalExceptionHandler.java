package com.disobaeva.hotelbooking.exception;

import com.disobaeva.hotelbooking.dto.response.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * Централизованное превращение исключений в JSON и HTTP-коды.
 *
 * <p>Благодаря общему предку {@link DomainException} контроллеры не обязаны
 * ловить ошибки вручную: достаточно бросить исключение в сервисе/домене.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Объект не найден → 404. */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(e.getErrorCode(), e.getMessage()));
    }

    /** Некорректные входные данные → 400. */
    @ExceptionHandler({InvalidBookingDatesException.class, RoomCapacityExceededException.class})
    public ResponseEntity<ErrorResponse> handleInvalidInput(BusinessRuleException e) {
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(e.getErrorCode(), e.getMessage()));
    }

    /** Конфликт с текущим состоянием системы → 409. */
    @ExceptionHandler({
            RoomNotAvailableException.class,
            InvalidBookingStateException.class,
            DuplicateEntityException.class
    })
    public ResponseEntity<ErrorResponse> handleConflict(BusinessRuleException e) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(e.getErrorCode(), e.getMessage()));
    }

    /** Остальные бизнес-правила → 409. */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException e) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(e.getErrorCode(), e.getMessage()));
    }

    /** Ошибки @Valid в DTO → 400 с перечнем полей. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String details = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse("VALIDATION_ERROR", details));
    }

    /** Битый JSON, неверный формат даты, неверный тип параметра → 400. */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception e) {
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse("BAD_REQUEST", "Некорректный формат запроса: " + e.getMessage()));
    }

    /** Не передан обязательный query-параметр → 400. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException e) {
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse("MISSING_PARAMETER",
                        "Не передан обязательный параметр: " + e.getParameterName()));
    }

    /** Неподдерживаемый HTTP-метод → 405. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ErrorResponse("METHOD_NOT_ALLOWED",
                        "Метод " + e.getMethod() + " не поддерживается для этого URL"));
    }

    /** Неподдерживаемый Content-Type → 415. */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException e) {
        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new ErrorResponse("UNSUPPORTED_MEDIA_TYPE",
                        "Ожидается Content-Type application/json"));
    }

    /** Нарушение уникальности/внешнего ключа на уровне БД → 409. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("Нарушение целостности данных", e);
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("DATA_INTEGRITY_VIOLATION",
                        "Операция нарушает ограничения базы данных"));
    }

    /** Неизвестный URL → 404, а не 500. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("NOT_FOUND", "Ресурс не найден: " + e.getResourcePath()));
    }

    /** Всё непредвиденное → 500, без раскрытия стека клиенту. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Непредвиденная ошибка", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "Внутренняя ошибка сервера"));
    }
}
