package com.disobaeva.hotelbooking.controller;

import com.disobaeva.hotelbooking.dto.BookingStatistics;
import com.disobaeva.hotelbooking.dto.request.BookingRequest;
import com.disobaeva.hotelbooking.dto.response.BookingResponse;
import com.disobaeva.hotelbooking.model.entity.Booking;
import com.disobaeva.hotelbooking.model.entity.BookingStatus;
import com.disobaeva.hotelbooking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

/** HTTP-адаптер для бронирований. */
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * POST /api/bookings
     * Тело: clientId, roomId, checkIn, checkOut, guestCount.
     */
    @PostMapping
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody BookingRequest request) {
        Booking booking = bookingService.create(
                request.clientId(),
                request.roomId(),
                request.checkIn(),
                request.checkOut(),
                request.guestCount());
        return ResponseEntity
                .created(URI.create("/api/bookings/" + booking.getId()))
                .body(BookingResponse.from(booking));
    }

    /** GET /api/bookings/{id} */
    @GetMapping("/{id}")
    public BookingResponse findById(@PathVariable Long id) {
        return BookingResponse.from(bookingService.getById(id));
    }

    /** GET /api/bookings?clientId=1&hotelId=1&status=CONFIRMED */
    @GetMapping
    public List<BookingResponse> search(@RequestParam(required = false) Long clientId,
                                        @RequestParam(required = false) Long hotelId,
                                        @RequestParam(required = false) BookingStatus status) {
        return bookingService.search(clientId, hotelId, status).stream()
                .map(BookingResponse::from)
                .toList();
    }

    /** POST /api/bookings/{id}/confirm */
    @PostMapping("/{id}/confirm")
    public BookingResponse confirm(@PathVariable Long id) {
        return BookingResponse.from(bookingService.confirm(id));
    }

    /** POST /api/bookings/{id}/cancel */
    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(@PathVariable Long id) {
        return BookingResponse.from(bookingService.cancel(id));
    }

    /** POST /api/bookings/{id}/complete — дополнительный переход состояния. */
    @PostMapping("/{id}/complete")
    public BookingResponse complete(@PathVariable Long id) {
        return BookingResponse.from(bookingService.complete(id));
    }

    /** GET /api/bookings/statistics?hotelId=1&from=2026-09-01&to=2026-09-30 */
    @GetMapping("/statistics")
    public BookingStatistics statistics(
            @RequestParam(required = false) Long hotelId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return bookingService.getStatistics(hotelId, from, to);
    }
}
