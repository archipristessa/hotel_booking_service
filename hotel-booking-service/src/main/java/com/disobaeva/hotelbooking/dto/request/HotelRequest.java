package com.disobaeva.hotelbooking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Данные для создания и полного изменения отеля. */
public record HotelRequest(
        @NotBlank(message = "Название отеля обязательно")
        @Size(max = 150, message = "Название отеля не длиннее 150 символов")
        String name,

        @NotBlank(message = "Город обязателен")
        @Size(max = 100, message = "Город не длиннее 100 символов")
        String city,

        @NotBlank(message = "Адрес обязателен")
        @Size(max = 255, message = "Адрес не длиннее 255 символов")
        String address
) {
}
