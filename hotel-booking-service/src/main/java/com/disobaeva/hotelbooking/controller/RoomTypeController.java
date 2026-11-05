package com.disobaeva.hotelbooking.controller;

import com.disobaeva.hotelbooking.dto.request.RoomTypeRequest;
import com.disobaeva.hotelbooking.dto.response.RoomTypeResponse;
import com.disobaeva.hotelbooking.model.entity.RoomType;
import com.disobaeva.hotelbooking.service.RoomTypeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/** HTTP-адаптер для справочника типов номеров. */
@RestController
@RequestMapping("/api/room-types")
public class RoomTypeController {

    private final RoomTypeService roomTypeService;

    public RoomTypeController(RoomTypeService roomTypeService) {
        this.roomTypeService = roomTypeService;
    }

    /** GET /api/room-types */
    @GetMapping
    public List<RoomTypeResponse> findAll() {
        return roomTypeService.findAll().stream()
                .map(RoomTypeResponse::from)
                .toList();
    }

    /** GET /api/room-types/{id} */
    @GetMapping("/{id}")
    public RoomTypeResponse findById(@PathVariable Long id) {
        return RoomTypeResponse.from(roomTypeService.getById(id));
    }

    /** POST /api/room-types */
    @PostMapping
    public ResponseEntity<RoomTypeResponse> create(@Valid @RequestBody RoomTypeRequest request) {
        RoomType roomType = roomTypeService.create(request.name());
        return ResponseEntity
                .created(URI.create("/api/room-types/" + roomType.getId()))
                .body(RoomTypeResponse.from(roomType));
    }
}
