package com.disobaeva.hotelbooking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Данные для создания типа номера. */
public record RoomTypeRequest(
        @NotBlank(message = "Название типа номера обязательно")
        @Size(max = 100, message = "Название не длиннее 100 символов")
        String name
) {
}
