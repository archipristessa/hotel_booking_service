# Hotel Booking Service (Тема 2: «Сервис бронирования отелей»)

Учебный проект по дисциплине «Объектно-ориентированное программирование».

**Реализованы все три этапа:** доменная модель, бизнес-логика, Spring Boot REST API,
JPA/Hibernate и PostgreSQL. Данные сохраняются после перезапуска приложения.

---

## Стек

| Технология | Версия | Зачем |
|---|---|---|
| Java | 25 | язык и runtime |
| Spring Boot | 3.5.16 | контейнер, REST, автоконфигурация |
| Spring Data JPA / Hibernate | 6.6.x | отображение объектов на таблицы |
| PostgreSQL | 17 | реляционная база данных |
| Maven | 3.9.16 (wrapper) | сборка и запуск |
| Postman | — | 23 запроса для ручной демонстрации |

Spring Boot 3.5.16 официально поддерживает Java 25.

---

## 1. Что должно быть установлено

### JDK 25

JDK 25 установлен через Homebrew:

```bash
brew install openjdk@25
```

В `~/.zshrc` добавлено:

```zsh
export JAVA_HOME="/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
```

Проверка:

```bash
source ~/.zshrc
java -version
# openjdk version "25.0.5" ...
```

> Если IntelliJ IDEA не видит JDK: `File → Project Structure → SDKs → Add SDK →
> JDK...` и выбрать `/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home`.

### PostgreSQL 17

```bash
brew install postgresql@17
brew services start postgresql@17
```

Проверка:

```bash
/opt/homebrew/opt/postgresql@17/bin/pg_isready
# /tmp:5432 - accepting connections
```

### База и пользователь

Один раз создайте роль и базу:

```bash
PSQL=/opt/homebrew/opt/postgresql@17/bin/psql

$PSQL -d postgres -c "CREATE ROLE hotel LOGIN PASSWORD 'hotel';"
/opt/homebrew/opt/postgresql@17/bin/createdb -O hotel hotel_booking
```

Проверка подключения:

```bash
PGPASSWORD=hotel /opt/homebrew/opt/postgresql@17/bin/psql \
  -h localhost -U hotel -d hotel_booking -c "select current_user, current_database();"
```

---

## 2. Запуск приложения

Из корня проекта `hotel-booking-service`:

```bash
source ~/.zshrc
./mvnw spring-boot:run
```

Приложение поднимется на `http://localhost:8080`.

Другие варианты:

```bash
# собрать исполняемый jar
./mvnw clean package

# запустить jar
java -jar target/hotel-booking-service-1.0.0-SNAPSHOT.jar
```

При первом запуске на пустой базе создаются демо-данные:

- типы номеров: Стандарт, Комфорт, Семейный, Люкс, Президентский;
- 5 отелей: Grand Hotel (Москва), Sea View (Сочи), Mountain Lodge
  (Красная Поляна), Business Plaza (Санкт-Петербург), Sunrise Resort (Казань);
- 21 номер;
- 10 клиентов;
- 25 бронирований во всех статусах: `CREATED`, `CONFIRMED`, `COMPLETED`,
  `CANCELLED`.

Если база уже содержит отели, демо-данные **не дублируются**.
Отключить инициализацию: `APP_SEED_ENABLED=false`.

### Настройки подключения к БД

`src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/hotel_booking}
    username: ${DB_USERNAME:hotel}
    password: ${DB_PASSWORD:hotel}
  jpa:
    hibernate:
      ddl-auto: update
```

Можно переопределить переменными окружения:

```bash
DB_URL=jdbc:postgresql://localhost:5432/hotel_booking \
DB_USERNAME=hotel \
DB_PASSWORD=hotel \
./mvnw spring-boot:run
```

---

## 3. Основной сценарий через REST API

Все примеры есть в файле [`requests.http`](requests.http) — его можно открыть
в IntelliJ IDEA и выполнять запросы по одному.

### 3.1. Создать тип номера

```bash
curl -X POST http://localhost:8080/api/room-types \
  -H 'Content-Type: application/json' \
  -d '{"name":"Апартаменты"}'
```

### 3.2. Создать отель

```bash
curl -X POST http://localhost:8080/api/hotels \
  -H 'Content-Type: application/json' \
  -d '{"name":"Sunrise Hotel","city":"Казань","address":"ул. Кремлёвская, 5"}'
```

Ответ `201 Created`:

```json
{"id":3,"name":"Sunrise Hotel","city":"Казань","address":"ул. Кремлёвская, 5","roomCount":0}
```

### 3.3. Добавить номер

```bash
curl -X POST http://localhost:8080/api/hotels/1/rooms \
  -H 'Content-Type: application/json' \
  -d '{"number":"401","roomTypeId":1,"capacity":2,"pricePerNight":5500}'
```

### 3.4. Найти свободные номера

```bash
curl "http://localhost:8080/api/rooms/available?hotelId=1&checkIn=2027-05-10&checkOut=2027-05-15&guestCount=2"
```

Результат — только свободные номера, подходящие по вместимости,
отсортированные по цене.

### 3.5. Создать бронирование

```bash
curl -X POST http://localhost:8080/api/bookings \
  -H 'Content-Type: application/json' \
  -d '{"clientId":1,"roomId":2,"checkIn":"2027-05-10","checkOut":"2027-05-15","guestCount":2}'
```

Ответ `201 Created`:

```json
{
  "bookingId": 2,
  "clientId": 1,
  "clientName": "Иван Петров",
  "roomId": 2,
  "roomNumber": "102",
  "hotelId": 1,
  "hotelName": "Grand Hotel",
  "checkIn": "2027-05-10",
  "checkOut": "2027-05-15",
  "guestCount": 2,
  "nights": 5,
  "pricePerNight": 3800.00,
  "total": 19000.00,
  "status": "CREATED"
}
```

### 3.6. Подтвердить бронирование

```bash
curl -X POST http://localhost:8080/api/bookings/2/confirm
```

### 3.7. Статистика

```bash
curl "http://localhost:8080/api/bookings/statistics?hotelId=1"
```

```json
{
  "totalBookings": 2,
  "totalRevenue": 40000.00,
  "countByStatus": {
    "CONFIRMED": 2
  }
}
```

### 3.8. Ошибки

Пересечение дат — `409 Conflict`:

```json
{
  "error": "BUSINESS_RULE_VIOLATION",
  "message": "Номер 2 недоступен на период 2027-05-10 — 2027-05-15: уже есть активное бронирование на пересекающиеся даты"
}
```

Превышение вместимости — `400 Bad Request`:

```json
{
  "error": "BUSINESS_RULE_VIOLATION",
  "message": "Номер вмещает 2 гостей, а запрошено 3"
}
```

Несуществующий отель — `404 Not Found`:

```json
{
  "error": "ENTITY_NOT_FOUND",
  "message": "Отель с идентификатором 9999 не найден"
}
```

---

## 4. REST API

### Отели и номера

| Метод | URL | Что делает |
|---|---|---|
| GET | `/api/hotels?city=&name=` | список отелей с фильтрами |
| GET | `/api/hotels/{id}` | отель по id |
| POST | `/api/hotels` | создать отель |
| PUT | `/api/hotels/{id}` | изменить отель |
| DELETE | `/api/hotels/{id}` | удалить отель (если нет броней) |
| GET | `/api/hotels/{id}/rooms?capacity=&minPrice=&maxPrice=` | номера отеля |
| POST | `/api/hotels/{id}/rooms` | добавить номер |
| GET | `/api/rooms/{id}` | номер по id |
| PUT | `/api/rooms/{id}` | изменить номер |
| GET | `/api/rooms/available?hotelId=&checkIn=&checkOut=&guestCount=` | свободные номера |

### Бронирования

| Метод | URL | Что делает |
|---|---|---|
| POST | `/api/bookings` | создать бронирование |
| GET | `/api/bookings/{id}` | бронирование по id |
| GET | `/api/bookings?clientId=&hotelId=&status=` | поиск броней |
| POST | `/api/bookings/{id}/confirm` | подтвердить |
| POST | `/api/bookings/{id}/cancel` | отменить |
| POST | `/api/bookings/{id}/complete` | завершить |
| GET | `/api/bookings/statistics?hotelId=&from=&to=` | количество и сумма |

### Справочники

| Метод | URL | Что делает |
|---|---|---|
| GET | `/api/room-types` | все типы номеров |
| GET | `/api/room-types/{id}` | тип по id |
| POST | `/api/room-types` | создать тип |
| GET | `/api/clients` | все клиенты |
| GET | `/api/clients/{id}` | клиент по id |
| POST | `/api/clients` | создать клиента |
| PUT | `/api/clients/{id}` | изменить клиента |
| DELETE | `/api/clients/{id}` | удалить клиента (если нет броней) |

### HTTP-коды

| Код | Когда |
|---|---|
| 200 OK | получение/изменение |
| 201 Created | создание (`Location` с URL нового объекта) |
| 204 No Content | удаление |
| 400 Bad Request | неверный JSON, даты, гости, `@Valid` |
| 404 Not Found | объект не найден |
| 409 Conflict | пересечение дат, неверный статус, дубликат email/номера |

---

## 5. Структура проекта

```
hotel-booking-service
├── pom.xml
├── reset-db.sh                      # очистка базы перед демонстрацией
├── POSTMAN_REQUESTS.md              # запросы для ручного создания в Postman
├── requests.http                    # HTTP-запросы для IntelliJ
├── postman/                         # готовая коллекция Postman на 23 запроса
│   ├── HotelBooking.postman_collection.json
│   ├── HotelBooking.postman_environment.json
│   └── README.md
├── README.md
├── PROJECT_GUIDE.md                 # подробный разбор архитектуры и кода
└── src
    └── main
        ├── java/com/disobaeva/hotelbooking
        │   ├── HotelBookingApplication.java   # @SpringBootApplication
        │   ├── config/DataInitializer.java     # демо-данные
        │   ├── controller/                     # REST-контроллеры
        │   ├── dto/                            # request/response + статистика
        │   ├── exception/                      # доменные исключения + GlobalExceptionHandler
        │   ├── model/                          # JPA-сущности
        │   ├── repository/                     # Spring Data JPA интерфейсы
        │   └── service/                        # бизнес-логика
        └── resources/application.yml
```

---

## 6. Проверка через Postman

Автотесты по требованию преподавателя не используются. Проверка выполняется
реальными HTTP-запросами через Postman.

Можно просто импортировать готовую коллекцию:

- `postman/HotelBooking.postman_collection.json` — 23 запроса;
- `postman/HotelBooking.postman_environment.json` — окружение.

В коллекции:

- 4 подготовительных запроса;
- 15 обязательных методов из таблицы задания;
- 4 ошибочных сценария.

Если хочешь создать запросы вручную — все URL и JSON есть в
[`POSTMAN_REQUESTS.md`](POSTMAN_REQUESTS.md).

Перед чистой демонстрацией можно очистить базу:

```bash
./reset-db.sh
```

После этого перезапусти приложение — демо-данные создадутся заново.

Альтернатива Postman — файл [`requests.http`](requests.http) для IntelliJ IDEA.

---

## 7. Схема базы данных

Hibernate создаёт таблицы автоматически (`ddl-auto: update`):

```sql
CREATE TABLE hotels (
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(150) NOT NULL,
    city    VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL
);

CREATE TABLE room_types (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE rooms (
    id              BIGSERIAL PRIMARY KEY,
    number          VARCHAR(20)   NOT NULL,
    capacity        INT           NOT NULL,
    price_per_night NUMERIC(10,2) NOT NULL,
    hotel_id        BIGINT        NOT NULL REFERENCES hotels (id),
    room_type_id    BIGINT        NOT NULL REFERENCES room_types (id),
    CONSTRAINT uk_room_hotel_number UNIQUE (hotel_id, number)
);

CREATE TABLE clients (
    id         BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE bookings (
    id              BIGSERIAL PRIMARY KEY,
    check_in        DATE          NOT NULL,
    check_out       DATE          NOT NULL,
    guest_count     INT           NOT NULL,
    price_per_night NUMERIC(10,2) NOT NULL,
    status          VARCHAR(20)   NOT NULL,
    client_id       BIGINT        NOT NULL REFERENCES clients (id),
    room_id         BIGINT        NOT NULL REFERENCES rooms (id)
);
```

Посмотреть реальные таблицы:

```bash
PGPASSWORD=hotel /opt/homebrew/opt/postgresql@17/bin/psql \
  -h localhost -U hotel -d hotel_booking -c "\dt"
```

---

## 8. Что реализовано по требованиям

- доменная модель и связи: `Hotel → Room → RoomType`, `Client → Booking`, `Room → Booking`;
- инкапсуляция: приватные поля, валидация в конструкторах/методах, нет `setStatus`;
- предметные методы: `Room.canAccommodate`, `Booking.overlaps`, `Booking.calculateTotal`,
  `Booking.confirm/cancel/complete`;
- `equals`/`hashCode`/`toString` у сущностей;
- `List` (`Hotel.rooms`, `Client.bookings`, результаты Stream), `Set` (`EnumSet` активных статусов),
  `Map` (`EnumMap` статистики);
- `Comparable` (`Room` по цене), `Comparator` (отели по названию);
- `Optional` для поиска, который может ничего не найти;
- Stream API: `filter`, `map`, `sorted`, `min`, `reduce`, `anyMatch`, `groupingBy`, `counting`;
- собственные исключения и централизованный `@RestControllerAdvice`;
- JPA-сущности, `@ManyToOne`/`@OneToMany`, Spring Data JPA;
- REST API с JSON, DTO и HTTP-кодами;
- PostgreSQL и сохранение данных после перезапуска.

Подробный разбор «что, зачем и почему» — в [`PROJECT_GUIDE.md`](PROJECT_GUIDE.md).
Ответы на вопросы к защите — в файле `../Ответы на вопросы к защите.md`.

---

## 9. Полезные замечания

- Если macOS не даёт запустить `./mvnw` после скачивания проекта из мессенджера:
  ```bash
  xattr -d com.apple.quarantine mvnw
  ```
- `spring.jpa.open-in-view: true` оставлен специально: контроллеры превращают
  сущности в DTO, и ленивые связи (`@ManyToOne(fetch = LAZY)`) догружаются
  в рамках HTTP-запроса. В production обычно переходят на DTO, собранные
  внутри транзакции сервиса, и отключают OSIV.
- `ddl-auto: update` удобен для учебного проекта. В реальном проекте схему
  управляют миграциями (Flyway/Liquibase).
