package com.disobaeva.hotelbooking.service;

import com.disobaeva.hotelbooking.exception.BusinessRuleException;
import com.disobaeva.hotelbooking.exception.DuplicateEntityException;
import com.disobaeva.hotelbooking.exception.HotelNotFoundException;
import com.disobaeva.hotelbooking.exception.InvalidBookingDatesException;
import com.disobaeva.hotelbooking.exception.RoomNotFoundException;
import com.disobaeva.hotelbooking.model.entity.Booking;
import com.disobaeva.hotelbooking.model.entity.BookingStatus;
import com.disobaeva.hotelbooking.model.entity.Room;
import com.disobaeva.hotelbooking.model.entity.RoomType;
import com.disobaeva.hotelbooking.repository.BookingRepository;
import com.disobaeva.hotelbooking.repository.HotelRepository;
import com.disobaeva.hotelbooking.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Бизнес-логика по номерам: изменение, поиск свободных номеров, аналитика цен. */
@Service
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final RoomTypeService roomTypeService;
    private final HotelRepository hotelRepository;

    public RoomService(RoomRepository roomRepository,
                       BookingRepository bookingRepository,
                       RoomTypeService roomTypeService,
                       HotelRepository hotelRepository) {
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.roomTypeService = roomTypeService;
        this.hotelRepository = hotelRepository;
    }

    public Room getById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new RoomNotFoundException(id));
    }

    public List<Room> findAll() {
        return roomRepository.findAll();
    }

    /** Изменение номера. Проверяет уникальность номера комнаты и отсутствие активных броней. */
    @Transactional
    public Room update(Long roomId, String number, int capacity,
                       BigDecimal pricePerNight, Long roomTypeId) {
        Room room = getById(roomId);
        RoomType roomType = roomTypeService.getById(roomTypeId);

        Long hotelId = room.getHotel().getId();
        roomRepository.findByHotelIdAndNumberIgnoreCase(hotelId, number)
                .filter(existing -> !existing.getId().equals(roomId))
                .ifPresent(existing -> {
                    throw new DuplicateEntityException(
                            "Номер комнаты '" + number + "' уже существует в отеле");
                });

        if (!bookingRepository
                .findByRoomIdAndStatusIn(roomId, BookingStatus.activeStatuses())
                .isEmpty()) {
            throw new BusinessRuleException(
                    "Нельзя менять характеристики номера id=" + roomId
                            + ": по нему есть активные бронирования");
        }

        room.update(number, capacity, pricePerNight, roomType);
        return roomRepository.save(room);
    }

    /**
     * Поиск свободных номеров отеля на период [checkIn, checkOut) для заданного
     * числа гостей.
     *
     * <p>Алгоритм: все номера отеля → помещаются ли гости → нет ли активной
     * брони с пересечением дат → сортировка по цене.
     */
    public List<Room> findAvailable(Long hotelId, LocalDate checkIn, LocalDate checkOut, int guestCount) {
        validatePeriod(checkIn, checkOut);
        if (guestCount <= 0) {
            throw new BusinessRuleException("Количество гостей должно быть положительным");
        }
        if (!hotelRepository.existsById(hotelId)) {
            throw new HotelNotFoundException(hotelId);
        }
        return roomRepository.findByHotelId(hotelId).stream()
                .filter(room -> room.canAccommodate(guestCount))
                .filter(room -> isFree(room, checkIn, checkOut))
                .sorted()
                .toList();
    }

    /**
     * Самый дешёвый свободный номер.
     * {@link Optional} обязателен: свободных номеров может не быть.
     */
    public Optional<Room> findCheapestAvailable(Long hotelId, LocalDate checkIn,
                                                LocalDate checkOut, int guestCount) {
        return findAvailable(hotelId, checkIn, checkOut, guestCount).stream()
                .min(Comparator.naturalOrder());
    }

    /** Средняя цена за ночь по номерам отеля (Stream: map + reduce). */
    public Optional<BigDecimal> getAveragePrice(Long hotelId) {
        if (!hotelRepository.existsById(hotelId)) {
            throw new HotelNotFoundException(hotelId);
        }
        List<Room> rooms = roomRepository.findByHotelId(hotelId);
        if (rooms.isEmpty()) {
            return Optional.empty();
        }
        BigDecimal sum = rooms.stream()
                .map(Room::getPricePerNight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Optional.of(sum.divide(
                BigDecimal.valueOf(rooms.size()), 2, RoundingMode.HALF_UP));
    }

    /** Свободен ли номер: нет активной брони с пересечением дат. */
    private boolean isFree(Room room, LocalDate checkIn, LocalDate checkOut) {
        return bookingRepository
                .findByRoomIdAndStatusIn(room.getId(), BookingStatus.activeStatuses())
                .stream()
                .noneMatch(existing -> existing.overlaps(checkIn, checkOut));
    }

    private void validatePeriod(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new InvalidBookingDatesException("Даты заезда и выезда обязательны");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidBookingDatesException("Дата выезда должна быть позже даты заезда");
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw new InvalidBookingDatesException("Дата заезда не может быть в прошлом");
        }
    }
}
