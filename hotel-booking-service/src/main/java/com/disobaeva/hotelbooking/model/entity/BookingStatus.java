package com.disobaeva.hotelbooking.model.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * Жизненный цикл бронирования.
 *
 * <pre>
 * CREATED --> CONFIRMED --> COMPLETED
 *    |            |
 *    v            v
 * CANCELLED   CANCELLED
 * </pre>
 *
 * <p>Переходы разрешены только через методы {@link Booking}: {@code confirm()},
 * {@code cancel()}, {@code complete()}. Свободного {@code setStatus} нет.
 */
public enum BookingStatus {
    CREATED,
    CONFIRMED,
    COMPLETED,
    CANCELLED;

    /**
     * Статусы, в которых бронь занимает номер.
     * {@link EnumSet} — множество: статусы уникальны, порядок не важен,
     * а память под него выделяется очень компактно.
     */
    public static Set<BookingStatus> activeStatuses() {
        return EnumSet.of(CREATED, CONFIRMED);
    }

    public boolean isActive() {
        return this == CREATED || this == CONFIRMED;
    }
}
