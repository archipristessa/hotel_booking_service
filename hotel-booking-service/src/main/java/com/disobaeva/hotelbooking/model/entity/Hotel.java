package com.disobaeva.hotelbooking.model.entity;

import com.disobaeva.hotelbooking.exception.BusinessRuleException;
import com.disobaeva.hotelbooking.model.AbstractEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Отель.
 *
 * <p>Связь «отель содержит несколько номеров» — композиция:
 * {@link #rooms} находится в отеле, а в {@link Room} есть обратная ссылка
 * {@code hotel}. Владелец связи — {@link Room}, потому что внешний ключ
 * {@code hotel_id} хранится в таблице {@code rooms}.
 */
@Entity
@Table(name = "hotels")
public class Hotel extends AbstractEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 255)
    private String address;

    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Room> rooms = new ArrayList<>();

    /** Требуется Hibernate. */
    protected Hotel() {
    }

    public Hotel(String name, String city, String address) {
        setName(name);
        setCity(city);
        setAddress(address);
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public String getAddress() {
        return address;
    }

    public void setName(String name) {
        this.name = requireText(name, "Название отеля");
    }

    public void setCity(String city) {
        this.city = requireText(city, "Город");
    }

    public void setAddress(String address) {
        this.address = requireText(address, "Адрес");
    }

    /** Наружу отдаётся только чтение, чтобы нельзя было обойти {@link #addRoom(Room)}. */
    public List<Room> getRooms() {
        return Collections.unmodifiableList(rooms);
    }

    /**
     * Добавляет номер в отель и поддерживает двустороннюю связь.
     *
     * @throws BusinessRuleException если номер комнаты уже используется в этом отеле
     */
    public void addRoom(Room room) {
        if (room == null) {
            throw new BusinessRuleException("Нельзя добавить null вместо номера");
        }
        boolean duplicate = rooms.stream()
                .anyMatch(existing -> existing.getNumber().equalsIgnoreCase(room.getNumber()));
        if (duplicate) {
            throw new BusinessRuleException(
                    "Номер комнаты '" + room.getNumber() + "' уже существует в отеле '" + name + "'");
        }
        rooms.add(room);
        room.assignHotel(this);
    }

    public void removeRoom(Room room) {
        if (rooms.remove(room)) {
            room.assignHotel(null);
        }
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException(fieldName + " не может быть пустым");
        }
        return value.trim();
    }

    @Override
    public String toString() {
        return "Hotel{id=" + getId() + ", name='" + name + "', city='" + city
                + "', address='" + address + "', rooms=" + rooms.size() + "}";
    }
}
