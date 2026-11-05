package com.disobaeva.hotelbooking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Данные для создания и полного изменения номера. */
public record RoomRequest(
        @NotBlank(message = "Номер комнаты обязателен")
        @Size(max = 20, message = "Номер комнаты не длиннее 20 символов")
        String number,

        @NotNull(message = "Идентификатор типа номера обязателен")
        Long roomTypeId,

        @Positive(message = "Вместимость должна быть положительной")
        int capacity,

        @NotNull(message = "Цена за ночь обязательна")
        @DecimalMin(value = "0.01", message = "Цена за ночь должна быть положительной")
        @Digits(integer = 8, fraction = 2, message = "Цена: до 8 цифр до запятой и 2 после")
        BigDecimal pricePerNight
) {
}
