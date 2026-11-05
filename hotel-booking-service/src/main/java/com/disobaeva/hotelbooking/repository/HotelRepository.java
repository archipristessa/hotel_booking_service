package com.disobaeva.hotelbooking.repository;

import com.disobaeva.hotelbooking.model.entity.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA автоматически реализует этот интерфейс.
 *
 * <p>Методы {@code save}, {@code findById}, {@code findAll}, {@code delete}
 * уже есть в {@link JpaRepository}. Дополнительные запросы выводятся из имени
 * метода: {@code findByCityIgnoreCase} → {@code where lower(city) = lower(?)}.
 */
@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long> {

    List<Hotel> findByCityIgnoreCase(String city);

    List<Hotel> findByNameContainingIgnoreCase(String namePart);
}
