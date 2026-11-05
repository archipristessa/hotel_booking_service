package com.disobaeva.hotelbooking.dto.response;

import com.disobaeva.hotelbooking.model.entity.Room;

import java.math.BigDecimal;

/** Ответ по номеру. Содержит только нужные поля, без циклических ссылок. */
public record RoomResponse(
        Long id,
        String number,
        int capacity,
        BigDecimal pricePerNight,
        Long hotelId,
        String hotelName,
        Long roomTypeId,
        String roomTypeName
) {
    public static RoomResponse from(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getNumber(),
                room.getCapacity(),
                room.getPricePerNight(),
                room.getHotel().getId(),
                room.getHotel().getName(),
                room.getRoomType().getId(),
                room.getRoomType().getName()
        );
    }
}
