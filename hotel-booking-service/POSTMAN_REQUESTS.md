# Postman: готовые запросы по одному на каждый метод

Этот файл — для **ручного создания запросов в Postman**. Ничего импортировать
не нужно: создаёшь запрос, копируешь метод, URL и JSON.

> Если ручное создание не нужно, импортируй готовую коллекцию:
> `postman/HotelBooking.postman_collection.json` +
> `postman/HotelBooking.postman_environment.json`. В ней те же 23 запроса,
> а id созданных объектов сохраняются автоматически.

## Состав

- **15 обязательных запросов** — ровно по таблице из задания;
- **4 подготовительных** — чтобы получить id типа номера и клиента;
- **4 ошибочных** — чтобы показать 400, 404 и 409.

Итого **23 запроса**. Этого достаточно.

---

## Перед началом

Приложение должно быть запущено:

```bash
cd /Users/daria/Documents/Study/oop/hotel-booking-service
./mvnw spring-boot:run
```

База уже заполнена демо-данными:

```text
5 отелей, 21 номер, 10 клиентов, 25 бронирований
```

Базовый URL:

```text
http://localhost:8080
```

### Id, которые используются в примерах

| Что | Id | Значение |
|---|---|---|
| `hotelId` | `1` | Grand Hotel, Москва |
| `roomTypeId` | `1` | Стандарт |
| `roomId` | `2` | номер 102, вместимость 2, 3800 руб. |
| `roomId` для PUT | `5` | номер 401, вместимость 2, 5200 руб. |
| `clientId` | `1` | Иван Петров |
| `bookingId` для GET | `1` | подтверждённая бронь |
| `bookingId` для cancel | `3` | подтверждённая бронь Марии |

Если id отличаются — посмотри их так:

```text
GET http://localhost:8080/api/hotels
GET http://localhost:8080/api/hotels/1/rooms
GET http://localhost:8080/api/clients
GET http://localhost:8080/api/bookings
```

### Как создать запрос в Postman

1. Нажми `+` → `New Request`.
2. Выбери метод (`GET`, `POST`, `PUT`, `DELETE`).
3. Вставь URL.
4. Для POST/PUT: вкладка `Body` → `raw` → тип `JSON`.
5. Для POST/PUT добавь заголовок:
   ```text
   Content-Type: application/json
   ```
6. Нажми `Send`.
7. Сохрани запрос в папку, например `Hotel Booking`.

---

# Часть 1. Подготовка данных

Эти запросы нужны, чтобы узнать существующие id. Их можно выполнить один раз.

## 1. Получить типы номеров

```text
GET http://localhost:8080/api/room-types
```

Ожидается `200 OK`. В ответе будет список, например:

```json
[
  {"id": 1, "name": "Стандарт"},
  {"id": 2, "name": "Комфорт"},
  {"id": 3, "name": "Семейный"},
  {"id": 4, "name": "Люкс"},
  {"id": 5, "name": "Президентский"}
]
```

Отсюда берём `roomTypeId = 1`.

## 2. Получить клиентов

```text
GET http://localhost:8080/api/clients
```

Ожидается `200 OK`. В ответе будет список клиентов. Отсюда берём
`clientId = 1` (Иван Петров).

## 3. Создать тип номера (необязательно)

```text
POST http://localhost:8080/api/room-types
Content-Type: application/json
```

Body:

```json
{
  "name": "Апартаменты"
}
```

Ожидается `201 Created`. В ответе будет `id` нового типа — его можно
использовать как `roomTypeId`.

> Если запускаешь повторно, тип уже существует → `409`. Это нормально.

## 4. Создать клиента (необязательно)

```text
POST http://localhost:8080/api/clients
Content-Type: application/json
```

Body:

```json
{
  "firstName": "Мария",
  "lastName": "Иванова",
  "email": "maria.demo@mail.ru"
}
```

Ожидается `201 Created`. В ответе будет `id` нового клиента — его можно
использовать как `clientId`.

> Если запускаешь повторно, email уже занят → `409`. Это нормально.

---

# Часть 2. Обязательные 15 методов

## 1. GET /api/hotels — список отелей с фильтром

```text
GET http://localhost:8080/api/hotels?city=Москва
```

Ожидается `200 OK`:

```json
[
  {
    "id": 1,
    "name": "Grand Hotel",
    "city": "Москва",
    "address": "ул. Тверская, 1",
    "roomCount": 5
  }
]
```

Можно фильтровать по части названия:

```text
GET http://localhost:8080/api/hotels?name=Grand
```

Что сказать: «Spring Data JPA находит отели по городу/названию,
контроллер возвращает DTO».

## 2. GET /api/hotels/{id} — отель по id

```text
GET http://localhost:8080/api/hotels/1
```

Ожидается `200 OK` с отелем Grand Hotel.

Если id не существует:

```text
GET http://localhost:8080/api/hotels/9999
```

Ожидается `404 Not Found`.

## 3. POST /api/hotels — создать отель

```text
POST http://localhost:8080/api/hotels
Content-Type: application/json
```

Body:

```json
{
  "name": "Demo Hotel",
  "city": "Москва",
  "address": "ул. Демонстрационная, 1"
}
```

Ожидается `201 Created`. В ответе будет `id` — например, `6`.
Запомни его: он понадобится для PUT и DELETE.

Пример ответа:

```json
{
  "id": 6,
  "name": "Demo Hotel",
  "city": "Москва",
  "address": "ул. Демонстрационная, 1",
  "roomCount": 0
}
```

## 4. PUT /api/hotels/{id} — изменить отель

```text
PUT http://localhost:8080/api/hotels/6
Content-Type: application/json
```

Body:

```json
{
  "name": "Demo Hotel & Spa",
  "city": "Москва",
  "address": "ул. Демонстрационная, 1"
}
```

Ожидается `200 OK` с изменённым названием.

> Вместо `6` подставь id из ответа POST-запроса.

## 5. DELETE /api/hotels/{id} — удалить отель

```text
DELETE http://localhost:8080/api/hotels/6
```

Ожидается `204 No Content`.

Отель должен быть без номеров и броней — поэтому удаляем только что
созданный Demo Hotel.

Если попробовать удалить отель с бронью:

```text
DELETE http://localhost:8080/api/hotels/1
```

Ожидается `409 Conflict`:

```json
{
  "error": "BUSINESS_RULE_VIOLATION",
  "message": "Нельзя удалить отель 'Grand Hotel': по нему есть бронирования"
}
```

## 6. GET /api/hotels/{id}/rooms — номера отеля с фильтром

```text
GET http://localhost:8080/api/hotels/1/rooms?capacity=2&minPrice=3000&maxPrice=7000
```

Ожидается `200 OK` — номера Grand Hotel вместимостью от 2, ценой 3000–7000,
отсортированные по цене.

Пример ответа:

```json
[
  {
    "id": 2,
    "number": "102",
    "capacity": 2,
    "pricePerNight": 3800.00,
    "hotelId": 1,
    "hotelName": "Grand Hotel",
    "roomTypeId": 1,
    "roomTypeName": "Стандарт"
  },
  {
    "id": 1,
    "number": "101",
    "capacity": 2,
    "pricePerNight": 4200.00,
    "hotelId": 1,
    "hotelName": "Grand Hotel",
    "roomTypeId": 1,
    "roomTypeName": "Стандарт"
  }
]
```

## 7. POST /api/hotels/{id}/rooms — создать номер

```text
POST http://localhost:8080/api/hotels/1/rooms
Content-Type: application/json
```

Body:

```json
{
  "number": "701",
  "roomTypeId": 1,
  "capacity": 2,
  "pricePerNight": 6000
}
```

Ожидается `201 Created`. В ответе будет `id` нового номера.

Пример:

```json
{
  "id": 22,
  "number": "701",
  "capacity": 2,
  "pricePerNight": 6000.00,
  "hotelId": 1,
  "hotelName": "Grand Hotel",
  "roomTypeId": 1,
  "roomTypeName": "Стандарт"
}
```

## 8. PUT /api/rooms/{id} — изменить номер

```text
PUT http://localhost:8080/api/rooms/5
Content-Type: application/json
```

Body:

```json
{
  "number": "401",
  "roomTypeId": 1,
  "capacity": 2,
  "pricePerNight": 5400
}
```

Ожидается `200 OK`.

Номер 5 — это Grand Hotel 401. У него есть только завершённая бронь,
поэтому изменение разрешено. Если у номера есть активная бронь,
сервис вернёт `409`.

## 9. GET /api/rooms/available — свободные номера

```text
GET http://localhost:8080/api/rooms/available?hotelId=1&checkIn=2027-05-10&checkOut=2027-05-15&guestCount=2
```

Ожидается `200 OK` — только свободные номера Grand Hotel на эти даты,
подходящие по вместимости, отсортированные по цене.

Что сказать: «Сервис фильтрует номера по вместимости, проверяет активные
брони на пересечение дат и сортирует через `Comparable`».

## 10. POST /api/bookings — создать бронирование

```text
POST http://localhost:8080/api/bookings
Content-Type: application/json
```

Body:

```json
{
  "clientId": 1,
  "roomId": 2,
  "checkIn": "2027-05-10",
  "checkOut": "2027-05-15",
  "guestCount": 2
}
```

Ожидается `201 Created`. В ответе будет `bookingId` — например, `26`.

Пример:

```json
{
  "bookingId": 26,
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

Запомни `bookingId`: он нужен для confirm и GET.

## 11. GET /api/bookings/{id} — бронь по id

```text
GET http://localhost:8080/api/bookings/1
```

Ожидается `200 OK` — бронь с `nights`, `pricePerNight`, `total`, `status`.

Если id не существует:

```text
GET http://localhost:8080/api/bookings/9999
```

Ожидается `404 Not Found`.

## 12. GET /api/bookings — поиск броней

```text
GET http://localhost:8080/api/bookings?hotelId=1&status=CONFIRMED
```

Ожидается `200 OK` — подтверждённые брони Grand Hotel.

Можно фильтровать по клиенту:

```text
GET http://localhost:8080/api/bookings?clientId=1
```

## 13. POST /api/bookings/{id}/confirm — подтвердить бронь

```text
POST http://localhost:8080/api/bookings/26/confirm
```

Ожидается `200 OK`, статус `CONFIRMED`.

> Вместо `26` подставь `bookingId` из ответа POST /api/bookings.

## 14. POST /api/bookings/{id}/cancel — отменить бронь

```text
POST http://localhost:8080/api/bookings/3/cancel
```

Ожидается `200 OK`, статус `CANCELLED`.

Бронь 3 — подтверждённая бронь Марии. После отмены номер снова свободен
на эти даты.

> Если хочешь отменить только что созданную бронь, подставь её id.
> Но тогда не подтверждай её заранее.

## 15. GET /api/bookings/statistics — статистика

```text
GET http://localhost:8080/api/bookings/statistics?hotelId=1
```

Ожидается `200 OK`:

```json
{
  "totalBookings": 10,
  "totalRevenue": 250000.00,
  "countByStatus": {
    "CREATED": 2,
    "CONFIRMED": 5,
    "COMPLETED": 1,
    "CANCELLED": 2
  }
}
```

Числа зависят от того, какие брони ты уже создал/подтвердил/отменил.
Главное — показать `totalBookings`, `totalRevenue` и `countByStatus`.

---

# Часть 3. Ошибочные сценарии

Эти запросы нужны, чтобы показать минимум два ошибочных сценария.
Лучше выполнять после создания и подтверждения брони из пункта 10/13.

## 1. Пересечение дат → 409

```text
POST http://localhost:8080/api/bookings
Content-Type: application/json
```

Body — те же даты и номер, что в уже подтверждённой брони:

```json
{
  "clientId": 1,
  "roomId": 2,
  "checkIn": "2027-05-10",
  "checkOut": "2027-05-15",
  "guestCount": 2
}
```

Ожидается `409 Conflict`:

```json
{
  "error": "BUSINESS_RULE_VIOLATION",
  "message": "Номер 2 недоступен на период 2027-05-10 — 2027-05-15: уже есть активное бронирование на пересекающиеся даты"
}
```

## 2. Гостей больше вместимости → 400

```text
POST http://localhost:8080/api/bookings
Content-Type: application/json
```

Body:

```json
{
  "clientId": 1,
  "roomId": 2,
  "checkIn": "2027-06-10",
  "checkOut": "2027-06-15",
  "guestCount": 3
}
```

Номер 102 вмещает 2 гостей, поэтому ожидается `400 Bad Request`:

```json
{
  "error": "BUSINESS_RULE_VIOLATION",
  "message": "Номер вмещает 2 гостей, а запрошено 3"
}
```

## 3. Несуществующий отель → 404

```text
GET http://localhost:8080/api/hotels/9999
```

Ожидается `404 Not Found`:

```json
{
  "error": "ENTITY_NOT_FOUND",
  "message": "Отель с идентификатором 9999 не найден"
}
```

## 4. Неверные даты → 400

```text
POST http://localhost:8080/api/bookings
Content-Type: application/json
```

Body:

```json
{
  "clientId": 1,
  "roomId": 2,
  "checkIn": "2027-05-15",
  "checkOut": "2027-05-10",
  "guestCount": 2
}
```

Ожидается `400 Bad Request`.

---

# Часть 4. Что показывать в каком порядке

1. `GET /api/hotels?city=Москва` — данные из БД.
2. `GET /api/hotels/1` — отель по id.
3. `POST /api/hotels` — создать Demo Hotel, запомнить id.
4. `PUT /api/hotels/{id}` — изменить его.
5. `GET /api/hotels/1/rooms?...` — фильтр номеров.
6. `POST /api/hotels/1/rooms` — создать номер 701.
7. `PUT /api/rooms/5` — изменить номер.
8. `GET /api/rooms/available?...` — свободные номера.
9. `POST /api/bookings` — создать бронь, запомнить `bookingId`.
10. `GET /api/bookings/{bookingId}` — показать `nights` и `total`.
11. `GET /api/bookings?hotelId=1&status=CONFIRMED` — поиск.
12. `POST /api/bookings/{bookingId}/confirm` — подтвердить.
13. `POST /api/bookings/3/cancel` — отменить другую бронь.
14. `GET /api/bookings/statistics?hotelId=1` — сумма и группировка.
15. `DELETE /api/hotels/{id}` — удалить Demo Hotel.
16. Ошибочные запросы: пересечение `409`, вместимость `400`,
    несуществующий отель `404`.

---

# Часть 5. Если что-то не работает

| Симптом | Решение |
|---|---|
| `Connection refused` | приложение не запущено: `./mvnw spring-boot:run` |
| `409` при повторном создании типа/клиента | это нормально, данные уже есть; очисти базу `./reset-db.sh` и перезапусти |
| `400` при бронировании | проверь, что `checkOut` позже `checkIn`, а гостей не больше вместимости |
| `409` при бронировании | номер уже занят на эти даты; возьми другие даты или другой номер |
| `404` | проверь id в URL |
| пустой ответ | проверь, что выбрал `Body → raw → JSON` и заголовок `Content-Type` |

Перед чистой демонстрацией:

```bash
cd /Users/daria/Documents/Study/oop/hotel-booking-service
./reset-db.sh
```

После этого перезапусти приложение — демо-данные создадутся заново.
