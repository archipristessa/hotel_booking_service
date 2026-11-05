package com.disobaeva.hotelbooking.service;

import com.disobaeva.hotelbooking.dto.BookingStatistics;
import com.disobaeva.hotelbooking.exception.BookingNotFoundException;
import com.disobaeva.hotelbooking.exception.RoomNotAvailableException;
import com.disobaeva.hotelbooking.model.entity.Booking;
import com.disobaeva.hotelbooking.model.entity.BookingStatus;
import com.disobaeva.hotelbooking.model.entity.Client;
import com.disobaeva.hotelbooking.model.entity.Room;
import com.disobaeva.hotelbooking.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Бизнес-логика бронирований: оформление, смена статуса, поиск и аналитика.
 *
 * <p>Сервис оркестрирует доменные объекты: ночи и стоимость считает
 * {@link Booking}, правила одного объекта — тоже домен. Задача сервиса —
 * достать объекты из репозиториев, проверить межобъектные правила
 * (пересечение дат) и сохранить.
 */
@Service
@Transactional(readOnly = true)
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ClientService clientService;
    private final RoomService roomService;

    public BookingService(BookingRepository bookingRepository,
                          ClientService clientService,
                          RoomService roomService) {
        this.bookingRepository = bookingRepository;
        this.clientService = clientService;
        this.roomService = roomService;
    }

    /**
     * Оформление бронирования.
     *
     * <p>Порядок проверок важен:
     * <ol>
     *   <li>находим клиента и номер (иначе — EntityNotFound);</li>
     *   <li>создаём {@link Booking}: домен проверит даты и вместимость;</li>
     *   <li>проверяем пересечение с активными бронями номера;</li>
     *   <li>только после всех проверок сохраняем.</li>
     * </ol>
     * Так при ошибке в БД не появляется «битая» запись.
     */
    @Transactional
    public Booking create(Long clientId, Long roomId, LocalDate checkIn,
                          LocalDate checkOut, int guestCount) {
        Client client = clientService.getById(clientId);
        Room room = roomService.getById(roomId);

        Booking booking = new Booking(room, client, checkIn, checkOut, guestCount);

        boolean conflict = bookingRepository
                .findByRoomIdAndStatusIn(roomId, BookingStatus.activeStatuses())
                .stream()
                .anyMatch(existing -> existing.overlaps(checkIn, checkOut));
        if (conflict) {
            throw new RoomNotAvailableException(roomId, checkIn, checkOut);
        }

        Booking saved = bookingRepository.save(booking);
        client.addBooking(saved);
        return saved;
    }

    public Booking getById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(id));
    }

    public List<Booking> findAll() {
        return bookingRepository.findAll();
    }

    public List<Booking> findByClientId(Long clientId) {
        return bookingRepository.findByClientId(clientId);
    }

    @Transactional
    public Booking confirm(Long id) {
        Booking booking = getById(id);
        booking.confirm();
        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking cancel(Long id) {
        Booking booking = getById(id);
        booking.cancel();
        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking complete(Long id) {
        Booking booking = getById(id);
        booking.complete();
        return bookingRepository.save(booking);
    }

    /** Поиск бронирований по фильтрам. Любой параметр может быть null. */
    public List<Booking> search(Long clientId, Long hotelId, BookingStatus status) {
        List<Booking> source;
        if (clientId != null) {
            source = bookingRepository.findByClientId(clientId);
        } else if (hotelId != null) {
            source = bookingRepository.findByRoomHotelId(hotelId);
        } else {
            source = bookingRepository.findAll();
        }

        return source.stream()
                .filter(booking -> clientId == null
                        || booking.getClient().getId().equals(clientId))
                .filter(booking -> hotelId == null
                        || booking.getRoom().getHotel().getId().equals(hotelId))
                .filter(booking -> status == null || booking.getStatus() == status)
                .toList();
    }

    /**
     * Аналитика: количество и сумма неотменённых бронирований + группировка
     * по статусу. Используются filter, map, reduce, groupingBy, counting.
     */
    public BookingStatistics getStatistics(Long hotelId, LocalDate from, LocalDate to) {
        List<Booking> source = hotelId == null
                ? bookingRepository.findAll()
                : bookingRepository.findByRoomHotelId(hotelId);

        List<Booking> filtered = source.stream()
                .filter(booking -> from == null || !booking.getCheckIn().isBefore(from))
                .filter(booking -> to == null || !booking.getCheckIn().isAfter(to))
                .toList();

        List<Booking> payable = filtered.stream()
                .filter(booking -> booking.getStatus() != BookingStatus.CANCELLED)
                .toList();

        long totalBookings = payable.size();

        BigDecimal totalRevenue = payable.stream()
                .map(Booking::calculateTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<BookingStatus, Long> countByStatus = filtered.stream()
                .collect(Collectors.groupingBy(
                        Booking::getStatus,
                        () -> new EnumMap<>(BookingStatus.class),
                        Collectors.counting()));

        return new BookingStatistics(totalBookings, totalRevenue, countByStatus);
    }

    /** Optional-поиск: удобно, когда отсутствие брони — не ошибка. */
    public Optional<Booking> findById(Long id) {
        return bookingRepository.findById(id);
    }
}
