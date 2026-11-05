package com.disobaeva.hotelbooking.dto;

import com.disobaeva.hotelbooking.model.entity.BookingStatus;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Результат аналитики по бронированиям.
 *
 * <p>Запись ({@code record}) здесь уместна: это неизменяемый «снимок» результата,
 * который не является сущностью и не имеет поведения. Компактный конструктор
 * делает карту статусов неизменяемой, чтобы результат нельзя было испортить извне.
 *
 * @param totalBookings количество неотменённых бронирований
 * @param totalRevenue  суммарная стоимость неотменённых бронирований
 * @param countByStatus количество бронирований по каждому статусу
 */
public record BookingStatistics(
        long totalBookings,
        BigDecimal totalRevenue,
        Map<BookingStatus, Long> countByStatus
) {
    public BookingStatistics {
        EnumMap<BookingStatus, Long> copy = new EnumMap<>(BookingStatus.class);
        if (countByStatus != null) {
            copy.putAll(countByStatus);
        }
        countByStatus = Collections.unmodifiableMap(copy);
    }
}
