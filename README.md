# java-explore-with-me

Многомодульный дипломный проект Explore With Me.

## Структура Maven

- `explore-with-me` — корневой модуль (packaging `pom`);
- `ewm-stats-service` — сервис статистики:
  - `stats-dto` — общие DTO (`EndpointHitDto`, `ViewStatsDto`);
  - `stats-server` — Spring Boot приложение для сбора/выдачи статистики;
  - `stats-client` — HTTP‑клиент (RestTemplate) для обращения к stats-server;
- `ewm-main-service` — основной сервис приложения.

## Stats-service

- POST `/hit` — сохранение хитов;
- GET `/stats` — получение статистики по интервалу, набору URI и flag `unique`;
- PostgreSQL подключение через `application.properties` + `schema.sql`;
- Dockerfile на `eclipse-temurin:21-jdk-alpine`;
- docker-compose.yml поднимает: `stats-db`, `stats-server`, `main-db`, `ewm-service`.

## Main-service

Основной сервис приложения "Explore With Me" с полной реализацией API:

- **Admin API** — управление пользователями, категориями, событиями, подборками
- **Public API** — публичный просмотр категорий, событий, подборок
- **Private API** — управление своими событиями и заявками на участие

### Дополнительная функциональность: Комментарии к событиям

Реализована функциональность комментариев к событиям:

- Пользователи могут оставлять комментарии к опубликованным событиям
- Автор комментария может редактировать и удалять свои комментарии
- Публичный просмотр комментариев к событиям
- Получение всех комментариев пользователя

#### API эндпоинты:

**Private API:**
- `POST /users/{userId}/comments/events/{eventId}` — создать комментарий
- `PATCH /users/{userId}/comments/{commentId}` — обновить комментарий
- `DELETE /users/{userId}/comments/{commentId}` — удалить комментарий
- `GET /users/{userId}/comments` — получить все комментарии пользователя

**Public API:**
- `GET /events/{eventId}/comments` — получить комментарии к событию
- `GET /events/{eventId}/comments/{commentId}` — получить комментарий по ID

#### Postman коллекция

Postman коллекция с тестами находится в файле `postman/comments.json`.

## Запуск

```bash
mvn clean install
docker-compose up --build
```

## Pull Request

[Ссылка на Pull Request из ветки feature-comments в main](https://github.com/gerych2/java-explore-with-m/pull/4)
