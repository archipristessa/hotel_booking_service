package com.disobaeva.hotelbooking.repository;

import com.disobaeva.hotelbooking.model.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Репозиторий номеров. */
@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByHotelId(Long hotelId);

    /** Поиск номера комнаты внутри отеля без учёта регистра. */
    Optional<Room> findByHotelIdAndNumberIgnoreCase(Long hotelId, String number);
}
