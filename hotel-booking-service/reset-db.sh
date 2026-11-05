#!/bin/zsh
# Полная очистка учебной базы. После запуска приложения демо-данные создадутся заново.
set -e

PSQL="/opt/homebrew/opt/postgresql@17/bin/psql"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-hotel_booking}"
DB_USERNAME="${DB_USERNAME:-hotel}"
DB_PASSWORD="${DB_PASSWORD:-hotel}"

echo "Очищаю базу ${DB_NAME} на ${DB_HOST}:${DB_PORT}..."

PGPASSWORD="${DB_PASSWORD}" "${PSQL}" \
  -h "${DB_HOST}" \
  -p "${DB_PORT}" \
  -U "${DB_USERNAME}" \
  -d "${DB_NAME}" \
  -v ON_ERROR_STOP=1 \
  -c "TRUNCATE bookings, rooms, hotels, clients, room_types RESTART IDENTITY CASCADE;"

echo "Готово. Перезапустите приложение: ./mvnw spring-boot:run"
