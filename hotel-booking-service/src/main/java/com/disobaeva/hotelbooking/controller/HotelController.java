package com.disobaeva.hotelbooking.controller;

import com.disobaeva.hotelbooking.dto.request.HotelRequest;
import com.disobaeva.hotelbooking.dto.request.RoomRequest;
import com.disobaeva.hotelbooking.dto.response.HotelResponse;
import com.disobaeva.hotelbooking.dto.response.RoomResponse;
import com.disobaeva.hotelbooking.model.entity.Hotel;
import com.disobaeva.hotelbooking.model.entity.Room;
import com.disobaeva.hotelbooking.service.HotelService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

/**
 * HTTP-адаптер для отелей и их номеров.
 *
 * <p>Контроллер не содержит бизнес-правил: он принимает параметры,
 * вызывает сервис и превращает сущности в DTO.
 */
@RestController
@RequestMapping("/api/hotels")
public class HotelController {

    private final HotelService hotelService;

    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    /** GET /api/hotels?city=Москва&name=Grand */
    @GetMapping
    public List<HotelResponse> findAll(@RequestParam(required = false) String city,
                                       @RequestParam(required = false) String name) {
        return hotelService.search(city, name).stream()
                .map(HotelResponse::from)
                .toList();
    }

    /** GET /api/hotels/{id} */
    @GetMapping("/{id}")
    public HotelResponse findById(@PathVariable Long id) {
        return HotelResponse.from(hotelService.getById(id));
    }

    /** POST /api/hotels */
    @PostMapping
    public ResponseEntity<HotelResponse> create(@Valid @RequestBody HotelRequest request) {
        Hotel hotel = hotelService.create(request.name(), request.city(), request.address());
        return ResponseEntity
                .created(URI.create("/api/hotels/" + hotel.getId()))
                .body(HotelResponse.from(hotel));
    }

    /** PUT /api/hotels/{id} */
    @PutMapping("/{id}")
    public HotelResponse update(@PathVariable Long id,
                                @Valid @RequestBody HotelRequest request) {
        Hotel hotel = hotelService.update(id, request.name(), request.city(), request.address());
        return HotelResponse.from(hotel);
    }

    /** DELETE /api/hotels/{id} */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        hotelService.delete(id);
    }

    /** GET /api/hotels/{id}/rooms?capacity=2&minPrice=3000&maxPrice=7000 */
    @GetMapping("/{id}/rooms")
    public List<RoomResponse> findRooms(@PathVariable Long id,
                                        @RequestParam(required = false) Integer capacity,
                                        @RequestParam(required = false) BigDecimal minPrice,
                                        @RequestParam(required = false) BigDecimal maxPrice) {
        return hotelService.getRooms(id, capacity, minPrice, maxPrice).stream()
                .map(RoomResponse::from)
                .toList();
    }

    /** POST /api/hotels/{id}/rooms */
    @PostMapping("/{id}/rooms")
    public ResponseEntity<RoomResponse> addRoom(@PathVariable Long id,
                                                @Valid @RequestBody RoomRequest request) {
        Room room = hotelService.addRoom(
                id,
                request.number(),
                request.roomTypeId(),
                request.capacity(),
                request.pricePerNight());
        return ResponseEntity
                .created(URI.create("/api/rooms/" + room.getId()))
                .body(RoomResponse.from(room));
    }
}
