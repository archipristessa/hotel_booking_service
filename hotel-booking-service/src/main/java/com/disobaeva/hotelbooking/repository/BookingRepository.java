package com.disobaeva.hotelbooking.repository;

import com.disobaeva.hotelbooking.model.entity.Booking;
import com.disobaeva.hotelbooking.model.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/** Репозиторий бронирований. */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByRoomId(Long roomId);

    List<Booking> findByClientId(Long clientId);

    /** Все бронирования отеля: booking.room.hotel.id. */
    List<Booking> findByRoomHotelId(Long hotelId);

    boolean existsByRoomHotelId(Long hotelId);

    /** Активные бронирования конкретного номера (CREATED/CONFIRMED). */
    List<Booking> findByRoomIdAndStatusIn(Long roomId, Collection<BookingStatus> statuses);
}
