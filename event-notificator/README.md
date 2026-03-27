# event-notificator

Сервис уведомлений, который получает изменения мероприятий из Kafka и отдает пользователю список непрочитанных уведомлений.

## API

### Уведомления

- `GET /notifications` — получить непрочитанные уведомления текущего пользователя.
- `POST /notifications` — пометить уведомления как прочитанные.

JWT пользователя должен передаваться в `Authorization: Bearer <token>`.

## Конфигурация (env)

Основные параметры можно задавать через переменные окружения.

- `EVENT_NOTIFICATOR_PORT` — порт приложения (по умолчанию `8083`).
- `POSTGRES_EVENT_NOTIFICATOR_PORT` — внешний порт Postgres (по умолчанию `5436`).
- `POSTGRES_EVENT_NOTIFICATOR_DB` — имя БД.
- `POSTGRES_EVENT_NOTIFICATOR_USER` / `POSTGRES_EVENT_NOTIFICATOR_PASSWORD` — пользователь и пароль БД.
- `EVENT_NOTIFICATOR_REDIS_HOST` / `EVENT_NOTIFICATOR_REDIS_PORT` — Redis для кэша и блокировок.
- `KAFKA_HOST_PORT` — внешний порт Kafka.
- `KAFKA_CONSUMER_GROUP_ID` — consumer group Kafka.
- `KAFKA_JSON_DEFAULT_TYPE` — тип входящего Kafka-сообщения.
- `KAFKA_TRUSTED_PACKAGES` — trusted packages для десериализации.
- `KAFKA_TOPIC_EVENT_CHANGED` — Kafka topic с изменениями мероприятий.
- `JWT_SECRET_KEY` — секретный ключ JWT.
- `JWT_LIFETIME_MS` — время жизни JWT.
- `EVENT_NOTIFICATOR_DEFAULT_PAGE_SIZE` / `EVENT_NOTIFICATOR_DEFAULT_PAGE_NUMBER` — пагинация по умолчанию.
- `EVENT_NOTIFICATOR_CACHE_DEFAULT_TTL` — базовый TTL кэша.
- `EVENT_NOTIFICATOR_CACHE_UNREAD_NOTIFICATIONS_TTL` — TTL кэша непрочитанных уведомлений.
- `EVENT_NOTIFICATOR_SCHEDULER_ENABLED` — включение планировщика очистки.
- `EVENT_NOTIFICATOR_NOTIFICATIONS_CLEANUP_CRON` — cron для очистки старых уведомлений.
- `EVENT_NOTIFICATOR_NOTIFICATIONS_TTL_DAYS` — срок хранения уведомлений в днях.

Liquibase changelog: `classpath:db/changelog/changelog-master.yaml`.

## Сборка и запуск

```bash
./mvnw -pl event-notificator -am clean package
```

Локальный запуск:

```bash
./mvnw -pl event-notificator -am spring-boot:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd -pl event-notificator -am clean package
.\mvnw.cmd -pl event-notificator -am spring-boot:run
```

## Docker

- `Dockerfile` лежит в `event-notificator/Dockerfile`.
- Сервис поднимается через общий `docker-compose.yaml`.
- В контейнере по умолчанию приложение доступно на порту `8083`.

## Swagger/UI

- `http://localhost:8083/swagger-ui.html`
