package com.disobaeva.hotelbooking.dto.response;

import com.disobaeva.hotelbooking.model.entity.RoomType;

/** Ответ по типу номера. */
public record RoomTypeResponse(
        Long id,
        String name
) {
    public static RoomTypeResponse from(RoomType roomType) {
        return new RoomTypeResponse(roomType.getId(), roomType.getName());
    }
}
