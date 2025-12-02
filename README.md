# java-explore-with-me

Многомодульный дипломный проект Explore With Me.

## Структура Maven

- `explore-with-me` — корневой модуль (packaging `pom`);
- `ewm-stats-service` — сервис статистики:
  - `stats-dto` — общие DTO (`EndpointHitDto`, `ViewStatsDto`);
  - `stats-server` — Spring Boot приложение для сбора/выдачи статистики;
  - `stats-client` — HTTP‑клиент (RestTemplate) для обращения к stats-server;
- `ewm-main-service` — основной сервис (пока заглушка с Actuator + Web).

## Stats-service

- POST `/hit` — сохранение хитов;
- GET `/stats` — получение статистики по интервалу, набору URI и flag `unique`;
- PostgreSQL подключение через `application.properties` + `schema.sql`;
- Dockerfile на `eclipse-temurin:21-jdk-alpine`;
- docker-compose.yml поднимает: `stats-db`, `stats-server`, `main-db`, `ewm-main-service`.

Запуск:
```bash
mvn clean install
docker-compose up --build
```

## Дополнительная функциональность (этап 1)

Выбранная тема: **комментарии к событиям**.

- Пользователь может оставлять комментарии к опубликованным событиям.
- Автор комментария может редактировать/удалять его до публикации.
- Инициатор события или администратор может модерировать (approve / reject).
- Планируемые поля таблицы `comments`:
  - `id`, `event_id`, `author_id`, `text`, `state`, `created`, `updated`.
- API:
  - публичное получение опубликованных комментариев по событию;
  - приватные операции пользователя (создать, обновить, удалить/отменить);
  - админ/инициатор — модерация и просмотр состояния.

Реализация самой фичи запланирована на последующих этапах.
