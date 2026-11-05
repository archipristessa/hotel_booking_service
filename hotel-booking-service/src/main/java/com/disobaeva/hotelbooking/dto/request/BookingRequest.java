package com.disobaeva.hotelbooking.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/** Данные для создания бронирования. */
public record BookingRequest(
        @NotNull(message = "Идентификатор клиента обязателен")
        Long clientId,

        @NotNull(message = "Идентификатор номера обязателен")
        Long roomId,

        @NotNull(message = "Дата заезда обязательна")
        @FutureOrPresent(message = "Дата заезда не может быть в прошлом")
        LocalDate checkIn,

        @NotNull(message = "Дата выезда обязательна")
        @Future(message = "Дата выезда должна быть в будущем")
        LocalDate checkOut,

        @Positive(message = "Количество гостей должно быть положительным")
        int guestCount
) {
}
