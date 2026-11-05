package com.disobaeva.hotelbooking.dto.response;

import com.disobaeva.hotelbooking.model.entity.Hotel;

/** Ответ по отелю без вложенных номеров (для списка и карточки). */
public record HotelResponse(
        Long id,
        String name,
        String city,
        String address,
        int roomCount
) {
    public static HotelResponse from(Hotel hotel) {
        return new HotelResponse(
                hotel.getId(),
                hotel.getName(),
                hotel.getCity(),
                hotel.getAddress(),
                hotel.getRooms().size()
        );
    }
}
