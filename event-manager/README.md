# event-manager

Основной сервис системы для управления пользователями, локациями, мероприятиями и регистрациями на события.

## API

### Пользователи

- `POST /users` — регистрация пользователя.
- `POST /users/auth` — аутентификация и получение JWT.
- `GET /users/{id}` — получить пользователя по id (только `ADMIN`).

### Локации

- `GET /locations` — получить список локаций.
- `GET /locations/{id}` — получить локацию по id.
- `POST /locations` — создать локацию (только `ADMIN`).
- `PUT /locations/{id}` — обновить локацию (только `ADMIN`).
- `DELETE /locations/{id}` — удалить локацию (только `ADMIN`).

### Мероприятия

- `GET /events/{id}` — получить мероприятие по id.
- `GET /events/my` — получить мероприятия, созданные текущим пользователем.
- `GET /events/registrations/my` — получить мероприятия, на которые записан текущий пользователь.
- `POST /events` — создать мероприятие.
- `POST /events/search` — поиск мероприятий по фильтру.
- `POST /events/registrations/{id}` — записаться на мероприятие.
- `PUT /events/{id}` — обновить мероприятие.
- `DELETE /events/{id}` — удалить мероприятие.
- `DELETE /events/registrations/cancel/{id}` — отменить регистрацию на мероприятие.

## Конфигурация (env)

Основные параметры можно задавать через переменные окружения.

- `EVENT_MANAGER_PORT` — порт приложения (по умолчанию `8082`).
- `POSTGRES_EVENT_MANAGER_PORT` — внешний порт Postgres (по умолчанию `5435`).
- `POSTGRES_EVENT_MANAGER_DB` — имя БД.
- `POSTGRES_EVENT_MANAGER_USER` / `POSTGRES_EVENT_MANAGER_PASSWORD` — пользователь и пароль БД.
- `EVENT_MANAGER_REDIS_HOST` / `EVENT_MANAGER_REDIS_PORT` — Redis для кэша и блокировок.
- `KAFKA_HOST_PORT` — внешний порт Kafka.
- `KAFKA_TOPIC_EVENT_CHANGED` — Kafka topic для событий изменения мероприятий.
- `JWT_SECRET_KEY` — секретный ключ JWT.
- `JWT_LIFETIME_MS` — время жизни JWT.
- `EVENT_MANAGER_DEFAULT_PAGE_SIZE` / `EVENT_MANAGER_DEFAULT_PAGE_NUMBER` — пагинация по умолчанию.
- `EVENT_MANAGER_CACHE_DEFAULT_TTL` — базовый TTL кэша.
- `EVENT_MANAGER_CACHE_EVENTS_TTL` — TTL кэша мероприятий.
- `EVENT_MANAGER_CACHE_LOCATIONS_TTL` — TTL кэша локаций.
- `EVENT_MANAGER_CACHE_REGISTRATIONS_TTL` — TTL кэша регистраций.
- `EVENT_MANAGER_CACHE_USERS_TTL` — TTL кэша пользователей.
- `EVENT_MANAGER_CACHE_USER_BOOKED_EVENTS_TTL` — TTL кэша мероприятий пользователя.
- `EVENT_MANAGER_CACHE_USER_CREATED_EVENTS_TTL` — TTL кэша созданных мероприятий пользователя.
- `EVENT_MANAGER_SCHEDULER_ENABLED` — включение планировщика.
- `EVENT_MANAGER_EVENT_STATUS_FIXED_DELAY_MS` — период обновления статусов мероприятий.

Liquibase changelog: `classpath:db/changelog/changelog-master.yaml`.

## Сборка и запуск

```bash
./mvnw -pl event-manager -am clean package
```

Локальный запуск:

```bash
./mvnw -pl event-manager -am spring-boot:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd -pl event-manager -am clean package
.\mvnw.cmd -pl event-manager -am spring-boot:run
```

## Docker

- `Dockerfile` лежит в `event-manager/Dockerfile`.
- Сервис поднимается через общий `docker-compose.yaml`.
- В контейнере по умолчанию приложение доступно на порту `8082`.

## Swagger/UI

- `http://localhost:8082/swagger-ui.html`
