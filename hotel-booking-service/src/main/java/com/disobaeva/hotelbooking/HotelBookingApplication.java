package com.disobaeva.hotelbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Точка входа Spring Boot-приложения.
 *
 * <p>Spring поднимает контейнер, сканирует пакет {@code com.disobaeva.hotelbooking},
 * находит контроллеры, сервисы и репозитории, создаёт их и внедряет зависимости.
 */
@SpringBootApplication
public class HotelBookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(HotelBookingApplication.class, args);
    }
}
