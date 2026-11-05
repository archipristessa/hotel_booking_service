package com.disobaeva.hotelbooking.model.entity;

import com.disobaeva.hotelbooking.exception.BusinessRuleException;
import com.disobaeva.hotelbooking.model.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

/**
 * Номер в отеле.
 *
 * <p>Реализует {@link Comparable}: естественный порядок — от дешёвых к дорогим,
 * поэтому в сервисах можно писать {@code rooms.stream().sorted()}.
 *
 * <p>Деньги хранятся в {@link BigDecimal}: {@code double} даёт ошибки округления,
 * недопустимые для стоимости проживания.
 */
@Entity
@Table(
        name = "rooms",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_room_hotel_number",
                columnNames = {"hotel_id", "number"}
        )
)
public class Room extends AbstractEntity implements Comparable<Room> {

    @Column(nullable = false, length = 20)
    private String number;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "price_per_night", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerNight;

    /** Владелец связи: в таблице rooms будет столбец hotel_id. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    /** Требуется Hibernate. */
    protected Room() {
    }

    public Room(String number, int capacity, BigDecimal pricePerNight, Hotel hotel, RoomType roomType) {
        validate(number, capacity, pricePerNight, hotel, roomType);
        this.number = number.trim();
        this.capacity = capacity;
        this.pricePerNight = pricePerNight;
        this.hotel = hotel;
        this.roomType = roomType;
    }

    public String getNumber() {
        return number;
    }

    public int getCapacity() {
        return capacity;
    }

    public BigDecimal getPricePerNight() {
        return pricePerNight;
    }

    public Hotel getHotel() {
        return hotel;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    /** Изменение характеристик с повторной проверкой всех правил. */
    public void update(String number, int capacity, BigDecimal pricePerNight, RoomType roomType) {
        validate(number, capacity, pricePerNight, hotel, roomType);
        this.number = number.trim();
        this.capacity = capacity;
        this.pricePerNight = pricePerNight;
        this.roomType = roomType;
    }

    /** Служебный метод для двусторонней связи (вызывается из {@link Hotel#addRoom(Room)}). */
    public void assignHotel(Hotel hotel) {
        this.hotel = hotel;
    }

    /** Помещается ли указанное число гостей в номер. */
    public boolean canAccommodate(int guestCount) {
        return guestCount > 0 && guestCount <= capacity;
    }

    @Override
    public int compareTo(Room other) {
        return this.pricePerNight.compareTo(other.pricePerNight);
    }

    private void validate(String number, int capacity, BigDecimal pricePerNight,
                          Hotel hotel, RoomType roomType) {
        if (number == null || number.isBlank()) {
            throw new BusinessRuleException("Номер комнаты не может быть пустым");
        }
        if (capacity <= 0) {
            throw new BusinessRuleException("Вместимость номера должна быть положительной");
        }
        if (pricePerNight == null || pricePerNight.signum() <= 0) {
            throw new BusinessRuleException("Цена за ночь должна быть положительной");
        }
        if (hotel == null) {
            throw new BusinessRuleException("Номер должен принадлежать отелю");
        }
        if (roomType == null) {
            throw new BusinessRuleException("У номера должен быть указан тип");
        }
    }

    @Override
    public String toString() {
        return "Room{id=" + getId() + ", number='" + number + "', capacity=" + capacity
                + ", pricePerNight=" + pricePerNight
                + ", type=" + (roomType == null ? null : roomType.getName()) + "}";
    }
}
