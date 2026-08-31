# JobFlow (Task Registry)

Распределённая система учёта фоновых и регламентных задач: очередь, исполнители-воркеры, планировщик (отдельный сервис), auth-service с JWT, журнал и веб-интерфейс через API Gateway.

## Архитектура (Фаза Auth)

| Сервис | Порт | Назначение |
|--------|------|------------|
| **gateway-impl** | 8080 | Static UI + маршрутизация API |
| **tasks-impl** | 8081 | Задачи, исполнители, логи, Kafka consumer |
| **scheduler-impl** | 8082 | Расписания, Kafka producer |
| **auth-impl** | 8083 | Login, users, выдача JWT |
| **Kafka** | 9092 | Команды `task.create` (локально или Docker) |

Аутентификация — **JWT (HS256)** с общим секретом. UI хранит `accessToken` в `sessionStorage` и отправляет `Authorization: Bearer …` на все API-запросы.

## Требования

- JDK 17+
- Maven 3.9+
- Kafka на `localhost:9092` (локальная установка или Docker — см. ниже)

## Сборка

```bash
mvn package
```

## Запуск

### 1. Kafka

**Если Kafka уже установлен локально** — ничего дополнительно не нужно. Сервисы подключаются к:

```properties
spring.kafka.bootstrap-servers=localhost:9092
```

Убедитесь, что брокер запущен. Топик `task.create` создастся автоматически, если у брокера включён `auto.create.topics.enable` (по умолчанию — да).

**Альтернатива — Docker** (если локальной Kafka нет):

```bash
docker compose up -d
```

Образ: `docker.redpanda.com/redpandadata/redpanda` (не Docker Hub). Подробнее — [Docker в РФ](#docker-в-рф).

### 2. JWT secret (рекомендуется)

Один и тот же секрет нужен **auth-impl**, **tasks-impl** и **scheduler-impl**:

```bash
# PowerShell
$env:APP_JWT_SECRET="your-256-bit-or-longer-secret-key-here!!"

# bash
export APP_JWT_SECRET="your-256-bit-or-longer-secret-key-here!!"
```

Если переменная не задана, используется dev-значение из `application.properties` (только для локальной разработки).

### 3. Backend-сервисы (4 терминала)

Запускайте из корня репозитория (чтобы `./data/*` были в одном месте):

```bash
mvn -pl auth/auth-impl spring-boot:run
mvn -pl tasks/tasks-impl spring-boot:run
mvn -pl scheduler/scheduler-impl spring-boot:run
mvn -pl gateway/gateway-impl spring-boot:run
```

### 4. Открыть UI

- Приложение: http://localhost:8080
- H2 Console tasks (только `ADMIN` + JWT): http://localhost:8081/h2-console  
  JDBC URL: `jdbc:h2:file:./data/taskdb`

### Учётные записи по умолчанию

| Логин | Пароль | Роль   |
|-------|--------|--------|
| admin | admin  | ADMIN  |
| user  | user   | VIEWER |

Смените пароли, если приложение доступно не только локально.

## Проверка API (E2E)

```bash
# 1. Login → accessToken
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'

# 2. Без token → 401
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/tasks

# 3. С token → 200
curl -s http://localhost:8080/api/tasks \
  -H "Authorization: Bearer <accessToken>"
```

Роли: **VIEWER** — только GET; **ADMIN** — POST/PATCH/DELETE.

## Структура monorepo

```
jobflow/
├── auth/auth-api         — DTO, UserRole, JWT helpers
├── auth/auth-impl        — login, users, JWT issuer
├── tasks/tasks-api       — контракт registry (TaskCreateCommand, TaskType, …)
├── tasks/tasks-impl      — registry + workers
├── scheduler/scheduler-api
├── scheduler/scheduler-impl
└── gateway/gateway-impl  — UI + Gateway
```

## Стек

- Java 17, Spring Boot 4.1
- Spring Cloud Gateway 2025.1
- Spring Security OAuth2 Resource Server (JWT HS256)
- Kafka, H2
- Static UI: HTML / CSS / JS

## Docker в РФ

Docker Hub с 2024 года ограничивает доступ с российских IP (403 Forbidden). Для JobFlow это не критично:

1. **Redpanda** — образ уже указан как `docker.redpanda.com/redpandadata/redpanda` (собственный registry вендора).
2. **Зеркала Docker Hub** — если понадобятся другие образы, добавьте в Docker Desktop → Settings → Docker Engine:

```json
{
  "registry-mirrors": [
    "https://dockerhub1.beget.com",
    "https://dockerhub.timeweb.cloud",
    "https://cr.yandex/mirror"
  ]
}
```

Нажмите **Apply & restart**. Актуальный список зеркал лучше сверять с документацией провайдера (Beget, Timeweb, Yandex Cloud).

3. **Без Docker** — можно поднять Kafka локально (см. [Apache Kafka quickstart](https://kafka.apache.org/quickstart)) и указать `spring.kafka.bootstrap-servers=localhost:9092` в сервисах.

## Автор

Кисоржевский Александр Дмитриевич — [ВКонтакте](https://vk.com/id13061960).
