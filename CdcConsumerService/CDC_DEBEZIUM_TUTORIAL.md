# CDC и Debezium: Полное руководство по изучению

## 📚 Теоретические основы

### Change Data Capture (CDC)
**CDC** - это паттерн для отслеживания изменений в базе данных и их передачи в другие системы в реальном времени.

**Ключевые принципы:**
- Мониторинг WAL (Write-Ahead Log) PostgreSQL
- Гарантированная доставка событий
- Минимальная нагрузка на основную БД
- Сохранение порядка операций

### Outbox Pattern
**Архитектурный паттерн** для надежной публикации событий:
- Бизнес-данные и события сохраняются в одной транзакции
- Исключает проблему "dual writes"
- Гарантирует консистентность данных

### Debezium
**Open-source платформа** для CDC:
- Коннекторы для разных БД (PostgreSQL, MySQL, MongoDB и др.)
- Интеграция с Kafka Connect
- Преобразование изменений в события Kafka

---

## 🏗️ Архитектура системы

```
PostgreSQL (WAL) → Debezium → Kafka → Spring Boot Consumer → OpenSearch
     ↑                                        ↓
  Outbox Table                         Business Logic
```

**Компоненты:**
1. **PostgreSQL** с включенным logical replication (`wal_level=logical`)
2. **Kafka + Zookeeper** для передачи событий
3. **Debezium Connect** для CDC коннектора
4. **Spring Boot Consumer** для обработки бизнес-логики
5. **OpenSearch** для аналитики и поиска

---

## 🛠️ Практическая реализация

### Шаг 1: Настройка инфраструктуры

**Docker Compose компоненты:**
- `postgres` с `wal_level=logical`
- `kafka` + `zookeeper`
- `debezium-connect` (порт 8083)
- `kafka-connect-ui` (порт 8000)
- `opensearch` + `opensearch-dashboards`

**Ключевая команда:**
```bash
docker-compose up -d
```

### Шаг 2: Подготовка базы данных

**Создание outbox таблицы:**
```sql
CREATE TABLE outbox_events (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    event_data JSONB NOT NULL,
    created_at BIGINT DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000000
);
```

**Важно:** Outbox таблица должна быть в той же БД, что и бизнес-данные!

### Шаг 3: Настройка Debezium коннектора

**REST API команда для создания коннектора:**
```bash
curl -X POST http://localhost:8083/connectors \
-H "Content-Type: application/json" \
-d @debezium-postgres-connector.json
```

**Ключевые параметры коннектора:**
- `database.hostname`, `database.port`, `database.user`
- `table.include.list: "public.outbox_events"`
- `plugin.name: "pgoutput"`
- `publication.autocreate.mode: "filtered"`

### Шаг 4: Spring Boot CDC Consumer

**Основные зависимости:**
- `spring-kafka` для Kafka integration
- `opensearch-java` для поиска и аналитики
- `jackson` для JSON парсинга

**Ключевые классы:**
- `DebeziumEvent<T>` - обертка для CDC событий
- `OutboxEventData` - структура outbox записи
- `OutboxEventConsumer` - Kafka listener
- `OpenSearchSyncService` - синхронизация с поисковой системой

---

## 🔧 Важные технические моменты

### PostgreSQL Configuration
```
wal_level = logical
max_wal_senders = 10  
max_replication_slots = 10
```

### Debezium Event Structure
```json
{
  "before": null,
  "after": { /* новые данные */ },
  "source": { /* метаданные */ },
  "op": "c|u|d|r",  // create/update/delete/read
  "ts_ms": 1234567890
}
```

### OpenSearch vs Elasticsearch
**Важное различие:**
- OpenSearch - форк Elasticsearch (Apache 2.0 license)
- Разные Java клиенты: `opensearch-java` vs `elasticsearch-java`
- API похожи, но клиенты несовместимы

---

## 📋 Команды для диагностики

### Проверка Kafka топиков:
```bash
docker exec kafka kafka-topics --list --bootstrap-server localhost:9092
```

### Проверка Debezium коннекторов:
```bash
curl http://localhost:8083/connectors
curl http://localhost:8083/connectors/postgres-outbox-connector/status
```

### Проверка OpenSearch индексов:
```powershell
Invoke-RestMethod -Uri "http://localhost:9200/_cat/indices?v"
```

### Поиск событий в OpenSearch:
```powershell
Invoke-RestMethod -Uri "http://localhost:9200/user-events-*/_search"
```

---

## 🎯 Типы событий и обработка

### Поддерживаемые события:
- `USER_CREATED` - создание пользователя
- `USER_UPDATED` - обновление профиля
- `USER_LOGIN` - вход в систему
- `ORDER_CREATED` - создание заказа
- `NOTIFICATION_SENT` - отправка уведомления

### Маршрутизация событий:
```java
switch (eventData.getEventType()) {
    case "USER_CREATED" -> handleUserCreated(eventData);
    case "USER_LOGIN" -> handleUserLogin(eventData);
    // и так далее
}
```

---

## 🚨 Проблемы и их решения

### 1. Коннектор исчезает
**Причина:** Ошибки в конфигурации или недоступность БД
**Решение:** Проверить логи и пересоздать коннектор

### 2. Content-Type ошибка 406
**Причина:** Использование Elasticsearch клиента с OpenSearch
**Решение:** Использовать правильный `opensearch-java` клиент

### 3. Данные не видны в Dashboard
**Причина:** Неправильный временной диапазон
**Решение:** Расширить время поиска или использовать Dev Tools

### 4. Неправильные timestamps
**Причина:** Использование `LocalDateTime` вместо `Instant`
**Решение:** Использовать `Instant.now().toString()` для UTC времени

---

## 📈 Мониторинг и отладка

### Kafka Connect UI (http://localhost:8000)
- Визуальный мониторинг коннекторов
- Просмотр конфигураций
- Статус и ошибки

### OpenSearch Dashboards (http://localhost:5601)
- Создание Index Patterns
- Discover для поиска событий
- Визуализация и дашборды

### Логи приложения
- Emoji-логирование для удобства отладки
- Структурированные сообщения
- Отслеживание CDC pipeline

---

## 🔄 Жизненный цикл события

1. **Бизнес-операция** → запись в outbox_events
2. **Debezium** читает WAL → создает Kafka событие
3. **Kafka** доставляет событие → Consumer получает
4. **Spring Boot** обрабатывает → бизнес-логика
5. **OpenSearch** индексирует → аналитика доступна

---

## 📝 Best Practices

### Проектирование:
- Используйте Outbox Pattern для transactional events
- Проектируйте события как immutable
- Добавляйте версионирование схем

### Производительность:
- Настройте батчинг в Kafka Consumer
- Используйте bulk операции для OpenSearch
- Мониторьте lag в Kafka

### Надежность:
- Настройте retry политики
- Используйте dead letter queues
- Реализуйте idempotent consumers

---

## 🎓 Заключение

Реализованная система демонстрирует:
- **Event-Driven Architecture** в действии
- **Микросервисную** интеграцию через события
- **Real-time analytics** с OpenSearch
- **Production-ready** подход к CDC

Эта архитектура подходит для:
- Audit логирования
- Real-time аналитики  
- Синхронизации между сервисами
- Event sourcing паттернов
- CQRS реализации

**Следующие шаги:** Изучение Kafka Streams, Schema Registry, Event sourcing паттернов. 