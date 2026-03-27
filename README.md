# EventManager

Микросервисное приложение для управления мероприятиями и уведомлениями пользователей.

## Архитектура

- `event-manager` — основной сервис для пользователей, локаций, мероприятий и регистраций.
- `event-notificator` — сервис уведомлений об изменениях мероприятий.
- `common-libs` — общие DTO, security-модели и вспомогательные классы.
- `docker-compose.yaml` — общий стек для локального запуска.

## Технологии

- Java 21
- Spring Boot 3
- Spring Security + JWT
- Spring Data JPA
- PostgreSQL
- Redis
- Kafka
- Liquibase
- Maven
- Docker Compose

## Требования

- Docker Desktop / Docker Engine + Compose
- JDK 21
- Maven Wrapper (`mvnw`, `mvnw.cmd`) уже лежит в проекте

## Режим разработки

1. Убедиться, что Docker запущен.
2. Поднять инфраструктуру и сервисы:

```bash
docker compose up --build -d
```

3. По умолчанию будут доступны:
   - `event-manager` — `http://localhost:8082`
   - `event-notificator` — `http://localhost:8083`
   - Postgres `event-manager` — `localhost:5435`
   - Postgres `event-notificator` — `localhost:5436`
   - Redis `event-manager` — `localhost:6379`
   - Redis `event-notificator` — `localhost:6380`
   - Kafka — `localhost:9092`

## Сборка проекта

```bash
./mvnw clean package
```

Windows PowerShell:

```powershell
.\mvnw.cmd clean package
```

## Локальный запуск сервисов без Docker

Перед запуском нужно отдельно поднять PostgreSQL, Redis и Kafka и выставить переменные окружения из `.env`.

macOS/Linux:

```bash
./mvnw -pl event-manager -am spring-boot:run
./mvnw -pl event-notificator -am spring-boot:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd -pl event-manager -am spring-boot:run
.\mvnw.cmd -pl event-notificator -am spring-boot:run
```

## Swagger/UI

- `event-manager` — `http://localhost:8082/swagger-ui.html`
- `event-notificator` — `http://localhost:8083/swagger-ui.html`

## Compose команды

- Запуск общего стека:

```bash
docker compose up --build -d
```

- Остановка контейнеров:

```bash
docker compose down
```

- Остановка с удалением томов:

```bash
docker compose down -v
```

- Просмотр логов:

```bash
docker compose logs -f
```
