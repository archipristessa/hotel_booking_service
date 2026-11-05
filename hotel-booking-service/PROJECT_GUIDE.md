# Подробный разбор проекта Hotel Booking Service

Документ объясняет, **что сделано, как работает и почему именно так** — чтобы
можно было уверенно защитить проект и ответить на вопросы по любой строчке.

---

## 1. Общая картина

Проект — Spring Boot-приложение с REST API, бизнес-логикой и PostgreSQL.
Запрос проходит по слоям:

```
HTTP request
     ↓
Controller        @RestController — принимает JSON, отдаёт JSON и HTTP-код
     ↓
Service           @Service — бизнес-правила, транзакции, оркестрация
     ↓
Repository        Spring Data JPA — запросы к БД
     ↓
PostgreSQL
```

Дополнительно:

```
Domain model      JPA-сущности: Hotel, Room, RoomType, Client, Booking
DTO               request/response — то, что реально летит по HTTP
Exception         доменные ошибки + GlobalExceptionHandler
```

### Почему так

- **Controller** не содержит бизнес-логики: он только принимает параметры,
  вызывает сервис и маппит сущность в DTO.
- **Service** отвечает за сценарий: достать объекты, проверить межобъектные
  правила, сохранить, управлять транзакцией.
- **Repository** не знает бизнес-правил — только как сохранить и найти.
- **Domain** содержит инварианты одного объекта: даты, вместимость, стоимость,
  переходы статуса.

Такое разделение позволяет тестировать домен без Spring и БД, менять БД
независимо от API и не дублировать правила в контроллерах.

---

## 2. Точка входа и конфигурация

### `HotelBookingApplication`

```java
@SpringBootApplication
public class HotelBookingApplication {
    public static void main(String[] args) {
        SpringApplication.run(HotelBookingApplication.class, args);
    }
}
```

`@SpringBootApplication` включает:

- `@Configuration` — класс может объявлять бины;
- `@EnableAutoConfiguration` — Spring Boot сам настраивает Tomcat, Jackson,
  DataSource, JPA, транзакции;
- `@ComponentScan` — сканирует пакет `com.disobaeva.hotelbooking` и находит
  `@RestController`, `@Service`, `@Repository`.

### `application.yml`

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/hotel_booking}
    username: ${DB_USERNAME:hotel}
    password: ${DB_PASSWORD:hotel}
  jpa:
    hibernate:
      ddl-auto: update
    open-in-view: true
    show-sql: true
```

- `ddl-auto: update` — Hibernate создаёт/дополняет таблицы.
- `open-in-view: true` — сессия Hibernate открыта на время HTTP-запроса,
  поэтому DTO можно собирать в контроллере, не ловя `LazyInitializationException`.
- SQL-логи включены, чтобы на защите было видно реальные запросы.

### `DataInitializer`

`CommandLineRunner` создаёт демо-данные, только если таблица `hotels` пуста.
Так при перезапуске данные не дублируются, а на пустой базе сразу есть что
показать. Сейчас создаётся 5 отелей, 21 номер, 10 клиентов и 25 бронирований
во всех статусах. Отключается свойством `app.seed.enabled=false`.

---

## 3. Доменная модель и JPA

### 3.1. `AbstractEntity`

```java
@MappedSuperclass
public abstract class AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof AbstractEntity that)) return false;
        if (Hibernate.getClass(this) != Hibernate.getClass(that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
```

Почему так:

- `@MappedSuperclass` — у класса нет своей таблицы, но `id` наследуется всеми
  сущностями.
- `@GeneratedValue(IDENTITY)` — id выдаёт PostgreSQL (`BIGSERIAL`).
- `equals` сравнивает **тип и id**. Два разных объекта с одним id — это одна
  и та же строка БД.
- `Hibernate.getClass(...)` нужен, потому что Hibernate может подсунуть
  прокси-подкласс; `getClass()` у прокси и у сущности различается.
- `hashCode` не зависит от id: id появляется только после `INSERT`.
  Если бы хеш менялся после сохранения, объект «терялся» бы в `HashSet`/`HashMap`.
  Стабильный хеш по классу — компромисс, безопасный для JPA.

### 3.2. `Hotel`

```java
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
}
```

- `@Entity` — класс хранится в таблице `hotels`.
- `@Table` — явное имя таблицы.
- `@OneToMany(mappedBy = "hotel")` — обратная сторона связи «отель → номера».
  Владелец — `Room`, потому что `hotel_id` хранится в таблице `rooms`.
- `cascade = ALL` — сохранение/удаление отеля каскадно применяется к номерам.
- `orphanRemoval = true` — номер, убранный из коллекции, удаляется.
- `getRooms()` возвращает `Collections.unmodifiableList(...)` — инкапсуляция.
- `addRoom()` проверяет уникальность номера комнаты через Stream `anyMatch`
  и поддерживает двустороннюю связь `room.assignHotel(this)`.

### 3.3. `Room`

```java
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;
}
```

- `@ManyToOne` — много номеров у одного отеля/типа. Владелец связи, здесь
  внешние ключи `hotel_id` и `room_type_id`.
- `@UniqueConstraint` — в одном отеле не может быть двух номеров с одним
  номером комнаты (`101`).
- `BigDecimal` для денег: `double` даёт ошибки округления.
- `Comparable<Room>` задаёт естественный порядок по цене, поэтому
  `rooms.stream().sorted()` сортирует по цене.
- `canAccommodate(guestCount)` — предметный метод, используется и в поиске,
  и в конструкторе `Booking`.

### 3.4. `RoomType`

Справочник типов: `Стандарт`, `Семейный`, `Люкс`.
`@Table(uniqueConstraints = ... name ...)` гарантирует уникальность названия
на уровне БД; сервис дополнительно проверяет без учёта регистра.

### 3.5. `Client`

```java
@Entity
@Table(name = "clients", uniqueConstraints = ... email ...)
public class Client extends AbstractEntity {

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @OneToMany(mappedBy = "client")
    private List<Booking> bookings = new ArrayList<>();
}
```

- Email хранится в нижнем регистре и проверяется регулярным выражением.
- Уникальность email — ограничение БД + проверка в `ClientService`.
- `@OneToMany(mappedBy = "client")` без каскадного удаления: бронирования —
  история, их нельзя удалять вместе с клиентом.

### 3.6. `Booking` — центральный объект

```java
@Entity
@Table(name = "bookings", indexes = {
    @Index(name = "idx_booking_room_dates", columnList = "room_id, check_in, check_out"),
    @Index(name = "idx_booking_client", columnList = "client_id")
})
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

    @Column(name = "price_per_night", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerNight;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;
}
```

Правила в конструкторе:

1. `room` и `client` не `null`;
2. `checkOut` строго позже `checkIn`;
3. `checkIn` не в прошлом;
4. `guestCount > 0` и `room.canAccommodate(guestCount)`;
5. `pricePerNight` берётся у номера **в момент создания** и дальше не меняется;
6. начальный статус — `CREATED`.

Методы:

- `getNights()` — `ChronoUnit.DAYS.between(checkIn, checkOut)`;
- `calculateTotal()` — `pricePerNight × nights`;
- `overlaps(a, b)` — `this.checkIn < b && a < this.checkOut`;
- `isActive()` — статус `CREATED` или `CONFIRMED`;
- `confirm()/cancel()/complete()` — переходы с проверкой текущего статуса.

`@Enumerated(EnumType.STRING)` хранит в БД строку `CREATED`, а не число.
Это устойчиво к изменению порядка enum.

### 3.7. `BookingStatus`

```java
public enum BookingStatus {
    CREATED, CONFIRMED, COMPLETED, CANCELLED;

    public static Set<BookingStatus> activeStatuses() {
        return EnumSet.of(CREATED, CONFIRMED);
    }

    public boolean isActive() {
        return this == CREATED || this == CONFIRMED;
    }
}
```

`EnumSet` — множество: статусы уникальны, порядок не важен, память компактная.
Используется в запросах «активные брони номера».

### 3.8. Схема связей

```
Hotel 1 ───── * Room
Room  * ───── 1 RoomType
Client 1 ──── * Booking
Room   1 ──── * Booking
Booking * ─── 1 Room
Booking * ─── 1 Client
```

В БД:

- `rooms.hotel_id → hotels.id`
- `rooms.room_type_id → room_types.id`
- `bookings.client_id → clients.id`
- `bookings.room_id → rooms.id`

---

## 4. Репозитории

Все репозитории — интерфейсы Spring Data JPA. Spring сам создаёт реализацию.

### `HotelRepository`

```java
public interface HotelRepository extends JpaRepository<Hotel, Long> {
    List<Hotel> findByCityIgnoreCase(String city);
    List<Hotel> findByNameContainingIgnoreCase(String namePart);
}
```

Методы выводятся из имени:

- `findByCityIgnoreCase` → `where upper(city) = upper(?)`;
- `findByNameContainingIgnoreCase` → `where upper(name) like upper('%?%')`.

### `RoomRepository`

```java
public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByHotelId(Long hotelId);
    Optional<Room> findByHotelIdAndNumberIgnoreCase(Long hotelId, String number);
}
```

`Optional` — потому что номера с таким номером комнаты может не быть.

### `ClientRepository`

```java
Optional<Client> findByEmailIgnoreCase(String email);
boolean existsByEmailIgnoreCase(String email);
```

### `RoomTypeRepository`

```java
boolean existsByNameIgnoreCase(String name);
Optional<RoomType> findByNameIgnoreCase(String name);
```

### `BookingRepository`

```java
List<Booking> findByRoomId(Long roomId);
List<Booking> findByClientId(Long clientId);
List<Booking> findByRoomHotelId(Long hotelId);
boolean existsByRoomHotelId(Long hotelId);
List<Booking> findByRoomIdAndStatusIn(Long roomId, Collection<BookingStatus> statuses);
```

`findByRoomHotelId` — вложенное свойство `booking.room.hotel.id`.
`findByRoomIdAndStatusIn` — активные брони конкретного номера.

Spring Data JPA даёт бесплатно: `save`, `findById`, `findAll`, `delete`,
`count`, `existsById`, пагинацию и сортировку.

---

## 5. Сервисы и бизнес-правила

### 5.1. Разделение «домен vs сервис»

- Правило касается **одного объекта** → метод домена:
  `room.canAccommodate`, `booking.overlaps`, `booking.calculateTotal`,
  `booking.confirm`.
- Правило требует посмотреть на **другие объекты** → сервис:
  «есть ли у номера другая активная бронь с пересечением дат»,
  «есть ли уже клиент с таким email», «не занят ли номер комнаты в отеле».

### 5.2. `BookingService.create`

```java
@Transactional
public Booking create(Long clientId, Long roomId, LocalDate checkIn,
                      LocalDate checkOut, int guestCount) {
    Client client = clientService.getById(clientId);
    Room room = roomService.getById(roomId);

    Booking booking = new Booking(room, client, checkIn, checkOut, guestCount);

    boolean conflict = bookingRepository
            .findByRoomIdAndStatusIn(roomId, BookingStatus.activeStatuses())
            .stream()
            .anyMatch(existing -> existing.overlaps(checkIn, checkOut));
    if (conflict) {
        throw new RoomNotAvailableException(roomId, checkIn, checkOut);
    }

    Booking saved = bookingRepository.save(booking);
    client.addBooking(saved);
    return saved;
}
```

Порядок важен:

1. найти клиента и номер;
2. создать `Booking` — домен проверит даты и вместимость;
3. проверить пересечение с активными бронями;
4. только потом `save`.

Если на шаге 2–3 выброшено исключение, в БД ничего не попало.
`@Transactional` гарантирует: либо всё сохранится, либо всё откатится.

### 5.3. `RoomService.findAvailable`

```java
public List<Room> findAvailable(Long hotelId, LocalDate checkIn, LocalDate checkOut, int guestCount) {
    validatePeriod(checkIn, checkOut);
    if (guestCount <= 0) {
        throw new BusinessRuleException("Количество гостей должно быть положительным");
    }
    if (!hotelRepository.existsById(hotelId)) {
        throw new HotelNotFoundException(hotelId);
    }
    return roomRepository.findByHotelId(hotelId).stream()
            .filter(room -> room.canAccommodate(guestCount))
            .filter(room -> isFree(room, checkIn, checkOut))
            .sorted()
            .toList();
}
```

`isFree`:

```java
return bookingRepository
        .findByRoomIdAndStatusIn(room.getId(), BookingStatus.activeStatuses())
        .stream()
        .noneMatch(existing -> existing.overlaps(checkIn, checkOut));
```

### 5.4. `HotelService`

- `create`, `getById`, `findAll`, `update`, `delete`;
- `delete` запрещает удалять отель с бронями;
- `search` использует производные запросы + Stream;
- `addRoom` создаёт номер и проверяет дубликат через домен;
- `getRooms` фильтрует по вместимости и цене, сортирует по цене.

### 5.5. `ClientService` и `RoomTypeService`

- создание с проверкой уникальности email/названия;
- `update` клиента с проверкой, что новый email не занят;
- `delete` клиента запрещён, если у него есть бронирования;
- `Optional` для поиска по email/названию.

### 5.6. Транзакции

- классы сервисов помечены `@Transactional(readOnly = true)`;
- изменяющие методы — `@Transactional`;
- `RuntimeException` (наши доменные исключения) по умолчанию откатывает транзакцию.

---

## 6. REST-слой

### 6.1. Контроллеры

| Контроллер | URL | Ответственность |
|---|---|---|
| `HotelController` | `/api/hotels` | отели и их номера |
| `RoomController` | `/api/rooms` | номер по id, изменение, свободные номера |
| `BookingController` | `/api/bookings` | создание, поиск, статусы, статистика |
| `ClientController` | `/api/clients` | CRUD клиентов |
| `RoomTypeController` | `/api/room-types` | справочник типов |

Пример:

```java
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody BookingRequest request) {
        Booking booking = bookingService.create(
                request.clientId(), request.roomId(),
                request.checkIn(), request.checkOut(), request.guestCount());
        return ResponseEntity
                .created(URI.create("/api/bookings/" + booking.getId()))
                .body(BookingResponse.from(booking));
    }
}
```

- `@RestController` = `@Controller` + `@ResponseBody`: возвращаемый объект
  сериализуется в JSON.
- `@RequestMapping` задаёт общий префикс.
- `@Valid` запускает валидацию DTO.
- `ResponseEntity.created(...)` возвращает `201` и заголовок `Location`.
- `BookingResponse.from(...)` — маппинг сущности в DTO.

### 6.2. DTO

Зачем:

- не отдавать наружу всю сущность и её ленивые связи;
- избежать циклических ссылок `Booking → Room → Hotel → rooms → ...`;
- не смешивать формат API и структуру БД;
- валидировать входные данные.

Пример `BookingRequest`:

```java
public record BookingRequest(
        @NotNull Long clientId,
        @NotNull Long roomId,
        @NotNull @FutureOrPresent LocalDate checkIn,
        @NotNull @Future LocalDate checkOut,
        @Positive int guestCount
) {
}
```

Пример `BookingResponse` — плоский JSON с `bookingId`, `nights`, `total`, `status`.

### 6.3. Полный сценарий `POST /api/bookings`

1. Jackson превращает JSON в `BookingRequest`.
2. `@Valid` проверяет поля; ошибка → `400`.
3. Контроллер вызывает `bookingService.create(...)`.
4. Сервис находит клиента и номер; нет → `404`.
5. `new Booking(...)` проверяет даты и вместимость; ошибка → `400`.
6. Сервис ищет активные брони номера с пересечением; есть → `409`.
7. `bookingRepository.save(...)` вставляет строку в `bookings`.
8. Контроллер возвращает `201` + `BookingResponse`.

---

## 7. Исключения и HTTP-коды

Иерархия:

```
RuntimeException
└── DomainException
    ├── EntityNotFoundException
    │   ├── HotelNotFoundException
    │   ├── RoomNotFoundException
    │   ├── RoomTypeNotFoundException
    │   ├── ClientNotFoundException
    │   └── BookingNotFoundException
    └── BusinessRuleException
        ├── RoomNotAvailableException
        ├── InvalidBookingDatesException
        ├── RoomCapacityExceededException
        ├── InvalidBookingStateException
        └── DuplicateEntityException
```

`DomainException` хранит `errorCode`, который попадает в JSON:

```json
{
  "error": "ENTITY_NOT_FOUND",
  "message": "Отель с идентификатором 9999 не найден"
}
```

`GlobalExceptionHandler`:

| Исключение | HTTP |
|---|---|
| `EntityNotFoundException` | 404 |
| `InvalidBookingDatesException` | 400 |
| `RoomCapacityExceededException` | 400 |
| `RoomNotAvailableException` | 409 |
| `InvalidBookingStateException` | 409 |
| `DuplicateEntityException` | 409 |
| `MethodArgumentNotValidException` | 400 |
| `HttpMessageNotReadableException` | 400 |
| `MissingServletRequestParameterException` | 400 |
| `HttpRequestMethodNotSupportedException` | 405 |
| `HttpMediaTypeNotSupportedException` | 415 |
| `DataIntegrityViolationException` | 409 |
| `NoResourceFoundException` | 404 |
| `Exception` | 500 |

Почему `RuntimeException`: ошибки предметной области — нормальный результат
неверного запроса, а не редкий сбой ввода-вывода. Не нужно засорять сигнатуры
`throws`; Spring по умолчанию откатывает транзакцию.

---

## 8. Stream API и Optional

| Требование | Где |
|---|---|
| Фильтрация | `RoomService.findAvailable`, `HotelService.getRooms/search`, `BookingService.search` |
| Поиск | `findById` → `Optional`, `findByEmail`, `findByHotelIdAndNumberIgnoreCase` |
| Сортировка | `Room implements Comparable`, `.sorted()`, `Comparator.comparing(Hotel::getName)` |
| Преобразование | `.map(Room::getPricePerNight)`, `.map(Booking::calculateTotal)`, DTO `.map(...)` |
| Группировка | `BookingService.getStatistics` → `Collectors.groupingBy` |
| Агрегация: количество | `.count()`, `Collectors.counting()` |
| Агрегация: сумма | `.reduce(BigDecimal.ZERO, BigDecimal::add)` |
| Агрегация: среднее | `RoomService.getAveragePrice` (сумма / количество) |
| Агрегация: минимум | `RoomService.findCheapestAvailable` → `.min(Comparator.naturalOrder())` |
| `Optional` | `findById`, `findByEmail`, `findCheapestAvailable`, `getAveragePrice` |

Пример группировки:

```java
Map<BookingStatus, Long> countByStatus = filtered.stream()
        .collect(Collectors.groupingBy(
                Booking::getStatus,
                () -> new EnumMap<>(BookingStatus.class),
                Collectors.counting()));
```

`EnumMap` выбран, чтобы ключи шли в порядке объявления enum и результат
был предсказуемым.

---

## 9. База данных

Hibernate создаёт схему автоматически. Основные ограничения:

- `clients.email` — `UNIQUE`;
- `room_types.name` — `UNIQUE`;
- `rooms(hotel_id, number)` — составной `UNIQUE`;
- `bookings.room_id → rooms.id`, `bookings.client_id → clients.id` — внешние ключи;
- индекс `idx_booking_room_dates` ускоряет поиск броней номера по датам.

Проверка:

```bash
PGPASSWORD=hotel /opt/homebrew/opt/postgresql@17/bin/psql \
  -h localhost -U hotel -d hotel_booking -c "\d bookings"
```

Данные сохраняются после перезапуска: это проверено — созданные бронирования
остались на месте после остановки и повторного запуска приложения.

---

## 10. Проверка через Postman

Автотесты по требованию преподавателя не используются. Работоспособность
проверяется реальными HTTP-запросами.

В проекте есть:

- `postman/HotelBooking.postman_collection.json` — готовая коллекция на
  23 запроса: 4 подготовительных, 15 обязательных и 4 ошибочных;
- `postman/HotelBooking.postman_environment.json` — окружение;
- `postman/README.md` — инструкция по импорту;
- `POSTMAN_REQUESTS.md` — те же запросы текстом, если нужно создать вручную;
- `requests.http` — те же запросы для IntelliJ IDEA.

Порядок:

1. Запустить приложение.
2. Импортировать коллекцию и окружение в Postman.
3. Выбрать окружение **Hotel Booking Local**.
4. Выполнить папки `01. Подготовка`, `02. Обязательные методы`, `03. Ошибки`.
5. Скрипты коллекции сами сохранят id созданных отеля, номера и брони.

Перед чистой демонстрацией:

```bash
./reset-db.sh
```

Скрипт очищает таблицы, после перезапуска приложения `DataInitializer`
создаёт демо-данные заново.

---

## 11. Сценарий демонстрации на защите

1. Запустить PostgreSQL: `brew services start postgresql@17`.
2. Запустить приложение: `./mvnw spring-boot:run`.
3. Показать `GET /api/hotels` — демо-данные уже в БД.
4. Создать отель, тип номера, номер, клиента через POST.
5. Показать `GET /api/rooms/available` — свободные номера на даты.
6. Создать бронирование через POST — показать `nights` и `total`.
7. Подтвердить бронь `POST /api/bookings/{id}/confirm`.
8. Показать `GET /api/bookings/statistics` — `groupingBy`/`reduce`.
9. Показать ошибки:
   - повторная бронь на те же даты → `409`;
   - количество гостей больше вместимости → `400`;
   - несуществующий отель → `404`.
10. Остановить приложение (`Ctrl+C`) и запустить снова.
11. Показать `GET /api/bookings` — данные сохранились.

---

## 12. Карта файлов

```
config/DataInitializer.java        демо-данные при пустой БД
controller/HotelController.java    отели + номера
controller/RoomController.java     номера + свободные номера
controller/BookingController.java  бронирования + статистика
controller/ClientController.java   клиенты
controller/RoomTypeController.java типы номеров
dto/request/*                      входные DTO с @Valid
dto/response/*                     выходные DTO
dto/BookingStatistics.java         результат аналитики
exception/*                        доменные исключения
exception/GlobalExceptionHandler   JSON ошибок + HTTP-коды
model/AbstractEntity.java          id, equals, hashCode
model/entity/Hotel.java            отель + List<Room>
model/entity/Room.java             номер + цена + Comparable
model/entity/RoomType.java         тип номера
model/entity/Client.java           клиент + email
model/entity/Booking.java          бронь + правила
model/entity/BookingStatus.java    enum + EnumSet активных статусов
repository/*                       Spring Data JPA интерфейсы
service/*                          бизнес-логика
```
