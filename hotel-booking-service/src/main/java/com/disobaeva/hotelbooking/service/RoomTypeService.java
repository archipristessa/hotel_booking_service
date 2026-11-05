package com.disobaeva.hotelbooking.service;

import com.disobaeva.hotelbooking.exception.DuplicateEntityException;
import com.disobaeva.hotelbooking.exception.RoomTypeNotFoundException;
import com.disobaeva.hotelbooking.model.entity.RoomType;
import com.disobaeva.hotelbooking.repository.RoomTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Бизнес-логика по типам номеров. */
@Service
@Transactional(readOnly = true)
public class RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;

    public RoomTypeService(RoomTypeRepository roomTypeRepository) {
        this.roomTypeRepository = roomTypeRepository;
    }

    /** Создаёт тип номера. Название должно быть уникальным без учёта регистра. */
    @Transactional
    public RoomType create(String name) {
        if (roomTypeRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateEntityException("Тип номера с названием '" + name + "' уже существует");
        }
        return roomTypeRepository.save(new RoomType(name));
    }

    public RoomType getById(Long id) {
        return roomTypeRepository.findById(id)
                .orElseThrow(() -> new RoomTypeNotFoundException(id));
    }

    /** Optional: тип с таким названием может отсутствовать — это нормальная ситуация поиска. */
    public Optional<RoomType> getByName(String name) {
        return roomTypeRepository.findByNameIgnoreCase(name);
    }

    public List<RoomType> findAll() {
        return roomTypeRepository.findAll();
    }
}
