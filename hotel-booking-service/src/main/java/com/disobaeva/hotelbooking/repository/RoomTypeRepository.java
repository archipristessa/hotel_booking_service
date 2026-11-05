package com.disobaeva.hotelbooking.repository;

import com.disobaeva.hotelbooking.model.entity.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Репозиторий типов номеров. */
@Repository
public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {

    boolean existsByNameIgnoreCase(String name);

    Optional<RoomType> findByNameIgnoreCase(String name);
}
