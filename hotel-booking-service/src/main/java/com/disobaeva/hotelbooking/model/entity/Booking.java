package com.disobaeva.hotelbooking.model.entity;

import com.disobaeva.hotelbooking.exception.InvalidBookingDatesException;
import com.disobaeva.hotelbooking.exception.InvalidBookingStateException;
import com.disobaeva.hotelbooking.exception.RoomCapacityExceededException;
import com.disobaeva.hotelbooking.model.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Бронирование номера клиентом на период [checkIn, checkOut).
 *
 * <p>Центральный объект предметной области. Здесь живут правила:
 * <ol>
 *   <li>дата выезда строго позже даты заезда, дата заезда не в прошлом;</li>
 *   <li>количество гостей положительно и не превышает вместимость номера;</li>
 *   <li>цена за ночь фиксируется в момент создания;</li>
 *   <li>переходы статуса выполняются только методами confirm/cancel/complete.</li>
 * </ol>
 *
 * <p>Пересечение периодов: {@code a.start < b.end && b.start < a.end}.
 * Период — полуинтервал [заезд, выезд), поэтому заезд в день чужого выезда
 * конфликтом не является.
 */
@Entity
@Table(
        name = "bookings",
        indexes = {
                @Index(name = "idx_booking_room_dates", columnList = "room_id, check_in, check_out"),
                @Index(name = "idx_booking_client", columnList = "client_id")
        }
)
public class Booking extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "check_in", nullable = false)
    private LocalDate checkIn;

    @Column(name = "check_out", nullable = false)
    private LocalDate checkOut;

    @Column(name = "guest_count", nullable = false)
    private int guestCount;

    /** «Замороженная» цена за ночь на момент оформления бронирования. */
    @Column(name = "price_per_night", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerNight;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    /** Требуется Hibernate. */
    protected Booking() {
    }

    public Booking(Room room, Client client, LocalDate checkIn, LocalDate checkOut, int guestCount) {
        if (room == null || client == null) {
            throw new InvalidBookingDatesException("Для бронирования нужны номер и клиент");
        }
        validateDates(checkIn, checkOut);
        validateGuestCount(room, guestCount);

        this.room = room;
        this.client = client;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guestCount = guestCount;
        this.pricePerNight = room.getPricePerNight();
        this.status = BookingStatus.CREATED;
    }

    public Room getRoom() {
        return room;
    }

    public Client getClient() {
        return client;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public int getGuestCount() {
        return guestCount;
    }

    public BigDecimal getPricePerNight() {
        return pricePerNight;
    }

    public BookingStatus getStatus() {
        return status;
    }

    /** Количество ночей = разница дат. Например, 10.09 -> 15.09 = 5 ночей. */
    public long getNights() {
        return ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /** Итоговая стоимость = ночи × зафиксированная цена за ночь. */
    public BigDecimal calculateTotal() {
        return pricePerNight.multiply(BigDecimal.valueOf(getNights()));
    }

    /** Пересекается ли это бронирование с указанным периодом. */
    public boolean overlaps(LocalDate otherCheckIn, LocalDate otherCheckOut) {
        return this.checkIn.isBefore(otherCheckOut) && otherCheckIn.isBefore(this.checkOut);
    }

    /** Активное бронирование занимает номер. Отменённое — нет. */
    public boolean isActive() {
        return status.isActive();
    }

    public void confirm() {
        if (status != BookingStatus.CREATED) {
            throw new InvalidBookingStateException(
                    "Подтвердить можно только бронирование в статусе CREATED, текущий статус: " + status);
        }
        status = BookingStatus.CONFIRMED;
    }

    public void cancel() {
        if (status == BookingStatus.CANCELLED) {
            throw new InvalidBookingStateException("Бронирование уже отменено");
        }
        if (status == BookingStatus.COMPLETED) {
            throw new InvalidBookingStateException("Завершённое бронирование нельзя отменить");
        }
        status = BookingStatus.CANCELLED;
    }

    public void complete() {
        if (status != BookingStatus.CONFIRMED) {
            throw new InvalidBookingStateException(
                    "Завершить можно только подтверждённое бронирование, текущий статус: " + status);
        }
        status = BookingStatus.COMPLETED;
    }

    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
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

    private void validateGuestCount(Room room, int guestCount) {
        if (guestCount <= 0) {
            throw new RoomCapacityExceededException("Количество гостей должно быть положительным");
        }
        if (!room.canAccommodate(guestCount)) {
            throw new RoomCapacityExceededException(room.getCapacity(), guestCount);
        }
    }

    @Override
    public String toString() {
        return "Booking{id=" + getId()
                + ", roomId=" + (room == null ? null : room.getId())
                + ", clientId=" + (client == null ? null : client.getId())
                + ", checkIn=" + checkIn + ", checkOut=" + checkOut
                + ", guestCount=" + guestCount
                + ", nights=" + getNights()
                + ", total=" + calculateTotal()
                + ", status=" + status + "}";
    }
}
