package com.disobaeva.hotelbooking.controller;

import com.disobaeva.hotelbooking.dto.request.RoomRequest;
import com.disobaeva.hotelbooking.dto.response.RoomResponse;
import com.disobaeva.hotelbooking.model.entity.Room;
import com.disobaeva.hotelbooking.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** HTTP-адаптер для конкретных номеров. */
@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    /** GET /api/rooms/{id} */
    @GetMapping("/{id}")
    public RoomResponse findById(@PathVariable Long id) {
        return RoomResponse.from(roomService.getById(id));
    }

    /** PUT /api/rooms/{id} */
    @PutMapping("/{id}")
    public RoomResponse update(@PathVariable Long id,
                               @Valid @RequestBody RoomRequest request) {
        Room room = roomService.update(
                id,
                request.number(),
                request.capacity(),
                request.pricePerNight(),
                request.roomTypeId());
        return RoomResponse.from(room);
    }

    /**
     * GET /api/rooms/available?hotelId=1&checkIn=2026-11-10&checkOut=2026-11-15&guestCount=2
     *
     * <p>Возвращает только свободные номера, подходящие по вместимости,
     * отсортированные по цене.
     */
    @GetMapping("/available")
    public List<RoomResponse> findAvailable(
            @RequestParam Long hotelId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam int guestCount) {
        return roomService.findAvailable(hotelId, checkIn, checkOut, guestCount).stream()
                .map(RoomResponse::from)
                .toList();
    }
}
