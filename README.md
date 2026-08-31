# JobFlow (Task Registry)

Распределённая система учёта фоновых и регламентных задач: очередь, исполнители-воркеры, планировщик (отдельный сервис), журнал и веб-интерфейс через API Gateway.

## Архитектура (Фаза 1)

| Сервис | Порт | Назначение |
|--------|------|------------|
| **gateway-impl** | 8080 | Static UI + маршрутизация API |
| **tasks-impl** | 8081 | Задачи, исполнители, логи, auth, Kafka consumer |
| **scheduler-impl** | 8082 | Расписания, Kafka producer |
| **Kafka** | 9092 | Команды `task.create` (локально или Docker) |

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

**Если Kafka уже установлен локально** — ничего дополнительно не нужно. Оба сервиса подключаются к:

```properties
spring.kafka.bootstrap-servers=localhost:9092
```

Убедитесь, что брокер запущен. Топик `task.create` создастся автоматически, если у брокера включён `auto.create.topics.enable` (по умолчанию — да).

**Альтернатива — Docker** (если локальной Kafka нет):

```bash
docker compose up -d
```

Образ: `docker.redpanda.com/redpandadata/redpanda` (не Docker Hub). Подробнее — [Docker в РФ](#docker-в-рф).

### 2. Backend-сервисы (3 терминала)

```bash
mvn -pl tasks/tasks-impl spring-boot:run
mvn -pl scheduler/scheduler-impl spring-boot:run
mvn -pl gateway/gateway-impl spring-boot:run
```

### 3. Открыть UI

- Приложение: http://localhost:8080
- H2 Console tasks (только `ADMIN`): http://localhost:8081/h2-console  
  JDBC URL: `jdbc:h2:file:./data/taskdb`

### Учётные записи по умолчанию

| Логин | Пароль | Роль   |
|-------|--------|--------|
| admin | admin  | ADMIN  |
| user  | user   | VIEWER |

Смените пароли, если приложение доступно не только локально.

## Структура monorepo

```
jobflow/
├── tasks/tasks-api      — контракт registry (TaskCreateCommand, TaskType, …)
├── tasks/tasks-impl     — registry
├── scheduler/scheduler-api
├── scheduler/scheduler-impl
└── gateway/gateway-impl — UI + Gateway
```

## Стек

- Java 17, Spring Boot 4.1
- Spring Cloud Gateway 2025.1
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

3. **Без Docker** — можно поднять Kafka локально (см. [Apache Kafka quickstart](https://kafka.apache.org/quickstart)) и указать `spring.kafka.bootstrap-servers=localhost:9092` в обоих сервисах.

## Автор

Кисоржевский Александр Дмитриевич — [ВКонтакте](https://vk.com/id13061960).
