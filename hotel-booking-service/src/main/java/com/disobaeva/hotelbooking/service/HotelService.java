package com.disobaeva.hotelbooking.service;

import com.disobaeva.hotelbooking.exception.BusinessRuleException;
import com.disobaeva.hotelbooking.exception.HotelNotFoundException;
import com.disobaeva.hotelbooking.model.entity.Hotel;
import com.disobaeva.hotelbooking.model.entity.Room;
import com.disobaeva.hotelbooking.model.entity.RoomType;
import com.disobaeva.hotelbooking.repository.BookingRepository;
import com.disobaeva.hotelbooking.repository.HotelRepository;
import com.disobaeva.hotelbooking.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/** Бизнес-логика по отелям и их номерам. */
@Service
@Transactional(readOnly = true)
public class HotelService {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final RoomTypeService roomTypeService;

    public HotelService(HotelRepository hotelRepository,
                        RoomRepository roomRepository,
                        BookingRepository bookingRepository,
                        RoomTypeService roomTypeService) {
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.roomTypeService = roomTypeService;
    }

    @Transactional
    public Hotel create(String name, String city, String address) {
        return hotelRepository.save(new Hotel(name, city, address));
    }

    public Hotel getById(Long id) {
        return hotelRepository.findById(id)
                .orElseThrow(() -> new HotelNotFoundException(id));
    }

    public List<Hotel> findAll() {
        return hotelRepository.findAll();
    }

    @Transactional
    public Hotel update(Long id, String name, String city, String address) {
        Hotel hotel = getById(id);
        hotel.setName(name);
        hotel.setCity(city);
        hotel.setAddress(address);
        return hotelRepository.save(hotel);
    }

    /**
     * Удаление отеля. Отель с бронированиями удалять нельзя: бронирования
     * ссылались бы на несуществующие номера. Номера удаляются каскадно
     * ({@code cascade = ALL, orphanRemoval = true} в {@link Hotel}).
     */
    @Transactional
    public void delete(Long id) {
        Hotel hotel = getById(id);
        if (bookingRepository.existsByRoomHotelId(id)) {
            throw new BusinessRuleException(
                    "Нельзя удалить отель '" + hotel.getName() + "': по нему есть бронирования");
        }
        hotelRepository.delete(hotel);
    }

    /**
     * Поиск отелей по городу и/или части названия. Любой параметр можно не указывать.
     * Используются производные запросы Spring Data + Stream API.
     */
    public List<Hotel> search(String city, String namePart) {
        boolean hasCity = city != null && !city.isBlank();
        boolean hasName = namePart != null && !namePart.isBlank();

        List<Hotel> hotels;
        if (hasCity && hasName) {
            String normalizedNamePart = namePart.trim().toLowerCase();
            hotels = hotelRepository.findByCityIgnoreCase(city.trim()).stream()
                    .filter(hotel -> hotel.getName().toLowerCase().contains(normalizedNamePart))
                    .toList();
        } else if (hasCity) {
            hotels = hotelRepository.findByCityIgnoreCase(city.trim());
        } else if (hasName) {
            hotels = hotelRepository.findByNameContainingIgnoreCase(namePart.trim());
        } else {
            hotels = hotelRepository.findAll();
        }

        return hotels.stream()
                .sorted(Comparator.comparing(Hotel::getName))
                .toList();
    }

    /**
     * Создание конкретного номера в отеле. Уникальность номера комнаты
     * проверяет доменный объект {@link Hotel#addRoom(Room)} и ограничение БД.
     */
    @Transactional
    public Room addRoom(Long hotelId, String number, Long roomTypeId,
                        int capacity, BigDecimal pricePerNight) {
        Hotel hotel = getById(hotelId);
        RoomType roomType = roomTypeService.getById(roomTypeId);
        Room room = new Room(number, capacity, pricePerNight, hotel, roomType);
        hotel.addRoom(room);
        return roomRepository.save(room);
    }

    /** Список номеров отеля с необязательными фильтрами по вместимости и цене. */
    public List<Room> getRooms(Long hotelId, Integer minCapacity,
                               BigDecimal minPrice, BigDecimal maxPrice) {
        getById(hotelId); // проверяем, что отель существует
        return roomRepository.findByHotelId(hotelId).stream()
                .filter(room -> minCapacity == null || room.getCapacity() >= minCapacity)
                .filter(room -> minPrice == null || room.getPricePerNight().compareTo(minPrice) >= 0)
                .filter(room -> maxPrice == null || room.getPricePerNight().compareTo(maxPrice) <= 0)
                .sorted()
                .toList();
    }
}
