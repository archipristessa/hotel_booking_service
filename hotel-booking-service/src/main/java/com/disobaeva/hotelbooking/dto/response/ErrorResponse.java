package com.disobaeva.hotelbooking.dto.response;

/** Единый формат ошибки REST API. */
public record ErrorResponse(
        String error,
        String message
) {
}
