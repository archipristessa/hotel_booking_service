package com.disobaeva.hotelbooking.config;

import com.disobaeva.hotelbooking.model.entity.Booking;
import com.disobaeva.hotelbooking.model.entity.BookingStatus;
import com.disobaeva.hotelbooking.model.entity.Client;
import com.disobaeva.hotelbooking.model.entity.Hotel;
import com.disobaeva.hotelbooking.model.entity.Room;
import com.disobaeva.hotelbooking.model.entity.RoomType;
import com.disobaeva.hotelbooking.repository.HotelRepository;
import com.disobaeva.hotelbooking.service.BookingService;
import com.disobaeva.hotelbooking.service.ClientService;
import com.disobaeva.hotelbooking.service.HotelService;
import com.disobaeva.hotelbooking.service.RoomService;
import com.disobaeva.hotelbooking.service.RoomTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Демонстрационные данные при первом запуске.
 *
 * <p>Если в базе уже есть отели, инициализация пропускается — данные не
 * дублируются. Отключить можно свойством {@code app.seed.enabled=false}
 * или переменной окружения {@code APP_SEED_ENABLED=false}.
 *
 * <p>Набор специально сделан «богатым», чтобы на защите было что показать:
 * 5 отелей в разных городах, 21 номер, 10 клиентов и 25 бронирований
 * во всех статусах (CREATED, CONFIRMED, COMPLETED, CANCELLED).
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final HotelRepository hotelRepository;
    private final RoomTypeService roomTypeService;
    private final HotelService hotelService;
    private final RoomService roomService;
    private final ClientService clientService;
    private final BookingService bookingService;

    public DataInitializer(HotelRepository hotelRepository,
                           RoomTypeService roomTypeService,
                           HotelService hotelService,
                           RoomService roomService,
                           ClientService clientService,
                           BookingService bookingService) {
        this.hotelRepository = hotelRepository;
        this.roomTypeService = roomTypeService;
        this.hotelService = hotelService;
        this.roomService = roomService;
        this.clientService = clientService;
        this.bookingService = bookingService;
    }

    @Override
    public void run(String... args) {
        if (hotelRepository.count() > 0) {
            log.info("Демо-данные не создаются: в базе уже есть {} отел(я/ей)", hotelRepository.count());
            return;
        }

        // ---------- Справочник типов номеров ----------
        RoomType standard = roomTypeService.create("Стандарт");
        RoomType comfort = roomTypeService.create("Комфорт");
        RoomType family = roomTypeService.create("Семейный");
        RoomType suite = roomTypeService.create("Люкс");
        RoomType president = roomTypeService.create("Президентский");

        // ---------- Отели ----------
        Hotel grand = hotelService.create(
                "Grand Hotel", "Москва", "ул. Тверская, 1");
        Hotel seaView = hotelService.create(
                "Sea View", "Сочи", "ул. Морская, 10");
        Hotel mountain = hotelService.create(
                "Mountain Lodge", "Красная Поляна", "ул. Горная, 5");
        Hotel business = hotelService.create(
                "Business Plaza", "Санкт-Петербург", "Невский проспект, 100");
        Hotel sunrise = hotelService.create(
                "Sunrise Resort", "Казань", "ул. Кремлёвская, 15");

        // ---------- Номера ----------
        Room g101 = addRoom(grand, "101", standard, 2, "4200");
        Room g102 = addRoom(grand, "102", standard, 2, "3800");
        Room g201 = addRoom(grand, "201", family, 4, "6500");
        Room g301 = addRoom(grand, "301", suite, 3, "9000");
        Room g401 = addRoom(grand, "401", comfort, 2, "5200");

        Room s1 = addRoom(seaView, "1", standard, 2, "5000");
        Room s2 = addRoom(seaView, "2", comfort, 3, "7200");
        Room s3 = addRoom(seaView, "3", family, 4, "9800");
        Room s4 = addRoom(seaView, "4", president, 4, "15000");

        Room m1 = addRoom(mountain, "A1", comfort, 2, "6100");
        Room m2 = addRoom(mountain, "A2", family, 4, "8800");
        Room m3 = addRoom(mountain, "B1", suite, 3, "11500");

        Room b1 = addRoom(business, "1001", standard, 1, "4500");
        Room b2 = addRoom(business, "1002", comfort, 2, "6900");
        Room b3 = addRoom(business, "1003", suite, 2, "12000");
        Room b4 = addRoom(business, "1004", president, 4, "21000");

        Room r1 = addRoom(sunrise, "12", standard, 2, "3900");
        Room r2 = addRoom(sunrise, "13", comfort, 3, "5700");
        Room r3 = addRoom(sunrise, "14", family, 4, "7600");
        Room r4 = addRoom(sunrise, "15", suite, 2, "10500");
        Room r5 = addRoom(sunrise, "16", president, 4, "18000");

        // ---------- Клиенты ----------
        Client ivan = clientService.create("Иван", "Петров", "ivan@mail.ru");
        Client anna = clientService.create("Анна", "Сидорова", "anna@mail.ru");
        Client maria = clientService.create("Мария", "Иванова", "maria@mail.ru");
        Client petr = clientService.create("Пётр", "Смирнов", "petr@mail.ru");
        Client elena = clientService.create("Елена", "Кузнецова", "elena@mail.ru");
        Client dmitry = clientService.create("Дмитрий", "Волков", "dmitry@mail.ru");
        Client olga = clientService.create("Ольга", "Морозова", "olga@mail.ru");
        Client sergey = clientService.create("Сергей", "Новиков", "sergey@mail.ru");
        Client tatyana = clientService.create("Татьяна", "Фёдорова", "tatyana@mail.ru");
        Client alexey = clientService.create("Алексей", "Козлов", "alexey@mail.ru");

        // ---------- Бронирования ----------
        // Даты считаются от сегодняшнего дня.
        LocalDate base = LocalDate.now();

        // Grand Hotel
        createBooking(ivan, g101, base.plusDays(10), base.plusDays(15), 2, BookingStatus.CONFIRMED);
        createBooking(anna, g102, base.plusDays(10), base.plusDays(15), 2, BookingStatus.CREATED);
        createBooking(maria, g201, base.plusDays(12), base.plusDays(17), 4, BookingStatus.CONFIRMED);
        createBooking(petr, g301, base.plusDays(20), base.plusDays(23), 3, BookingStatus.CANCELLED);
        createBooking(elena, g401, base.plusDays(5), base.plusDays(8), 2, BookingStatus.COMPLETED);

        // Sea View
        createBooking(dmitry, s1, base.plusDays(10), base.plusDays(14), 2, BookingStatus.CONFIRMED);
        createBooking(olga, s2, base.plusDays(18), base.plusDays(22), 3, BookingStatus.CREATED);
        createBooking(sergey, s3, base.plusDays(25), base.plusDays(30), 4, BookingStatus.CANCELLED);
        createBooking(tatyana, s4, base.plusDays(7), base.plusDays(9), 2, BookingStatus.CONFIRMED);

        // Mountain Lodge
        createBooking(alexey, m1, base.plusDays(14), base.plusDays(18), 2, BookingStatus.CONFIRMED);
        createBooking(ivan, m2, base.plusDays(20), base.plusDays(25), 4, BookingStatus.CREATED);
        createBooking(anna, m3, base.plusDays(30), base.plusDays(34), 3, BookingStatus.CANCELLED);

        // Business Plaza
        createBooking(maria, b1, base.plusDays(3), base.plusDays(6), 1, BookingStatus.COMPLETED);
        createBooking(petr, b2, base.plusDays(11), base.plusDays(15), 2, BookingStatus.CONFIRMED);
        createBooking(elena, b3, base.plusDays(20), base.plusDays(24), 2, BookingStatus.CREATED);
        createBooking(dmitry, b4, base.plusDays(40), base.plusDays(45), 4, BookingStatus.CONFIRMED);

        // Sunrise Resort
        createBooking(olga, r1, base.plusDays(9), base.plusDays(12), 2, BookingStatus.CONFIRMED);
        createBooking(sergey, r2, base.plusDays(15), base.plusDays(19), 3, BookingStatus.CANCELLED);
        createBooking(tatyana, r3, base.plusDays(22), base.plusDays(27), 4, BookingStatus.CREATED);
        createBooking(alexey, r4, base.plusDays(35), base.plusDays(38), 2, BookingStatus.CONFIRMED);
        createBooking(ivan, r5, base.plusDays(50), base.plusDays(55), 4, BookingStatus.COMPLETED);

        // Повторные брони на те же номера, но на непересекающиеся даты —
        // чтобы была видна история и разные статусы.
        createBooking(anna, g101, base.plusDays(40), base.plusDays(43), 2, BookingStatus.CREATED);
        createBooking(maria, g102, base.plusDays(30), base.plusDays(33), 2, BookingStatus.CANCELLED);
        createBooking(petr, s1, base.plusDays(30), base.plusDays(33), 2, BookingStatus.CREATED);
        createBooking(elena, g201, base.plusDays(40), base.plusDays(44), 3, BookingStatus.CONFIRMED);

        log.info("Демо-данные созданы: отелей — {}, номеров — {}, клиентов — {}, бронирований — {}",
                hotelRepository.count(),
                roomService.findAll().size(),
                clientService.findAll().size(),
                bookingService.findAll().size());
        log.info("Примеры: Grand Hotel — {} номеров, Business Plaza — {} номеров",
                hotelService.getRooms(grand.getId(), null, null, null).size(),
                hotelService.getRooms(business.getId(), null, null, null).size());
    }

    private Room addRoom(Hotel hotel, String number, RoomType roomType,
                         int capacity, String price) {
        return hotelService.addRoom(
                hotel.getId(), number, roomType.getId(), capacity, new BigDecimal(price));
    }

    /**
     * Создаёт бронь и переводит её в нужный статус.
     * Даты подобраны так, чтобы активные брони одного номера не пересекались.
     */
    private Booking createBooking(Client client, Room room,
                                  LocalDate checkIn, LocalDate checkOut,
                                  int guestCount, BookingStatus targetStatus) {
        Booking booking = bookingService.create(
                client.getId(), room.getId(), checkIn, checkOut, guestCount);

        switch (targetStatus) {
            case CONFIRMED -> bookingService.confirm(booking.getId());
            case COMPLETED -> {
                bookingService.confirm(booking.getId());
                bookingService.complete(booking.getId());
            }
            case CANCELLED -> bookingService.cancel(booking.getId());
            case CREATED -> {
                // уже CREATED
            }
        }
        return booking;
    }
}
