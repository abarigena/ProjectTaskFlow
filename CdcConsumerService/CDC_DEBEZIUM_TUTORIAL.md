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

## 🛠️ Практическая реализация (по нашим шагам)

### Шаг 1: Расширение Docker Compose

**Что добавили в docker-compose.yml:**
```yaml
  # Debezium Connect для CDC
  debezium-connect:
    image: debezium/connect:2.4
    container_name: debezium-connect
    ports:
      - "8083:8083"
    environment:
      - BOOTSTRAP_SERVERS=kafka:29092
      - GROUP_ID=debezium
      - CONFIG_STORAGE_TOPIC=debezium_configs
      - OFFSET_STORAGE_TOPIC=debezium_offsets
      - STATUS_STORAGE_TOPIC=debezium_status
    depends_on:
      - kafka
      - postgres

  # UI для мониторинга Kafka Connect
  kafka-connect-ui:
    image: landoop/kafka-connect-ui:0.9.7
    container_name: kafka-connect-ui
    ports:
      - "8000:8000"
    environment:
      - CONNECT_URL=http://debezium-connect:8083
    depends_on:
      - debezium-connect

  # OpenSearch вместо Elasticsearch
  opensearch:
    image: opensearchproject/opensearch:2.10.0
    container_name: opensearch
    ports:
      - "9200:9200"
      - "9600:9600"
    environment:
      - discovery.type=single-node
      - DISABLE_SECURITY_PLUGIN=true
      - "OPENSEARCH_JAVA_OPTS=-Xms512m -Xmx512m"

  # Dashboard для OpenSearch
  opensearch-dashboards:
    image: opensearchproject/opensearch-dashboards:2.10.0
    container_name: opensearch-dashboards
    ports:
      - "5601:5601"
    environment:
      - OPENSEARCH_HOSTS=http://opensearch:9200
      - DISABLE_SECURITY_DASHBOARDS_PLUGIN=true
    depends_on:
      - opensearch
```

**Важное изменение в PostgreSQL:**
```yaml
postgres:
  environment:
    - POSTGRES_DB=taskflow_db
    - POSTGRES_USER=postgres 
    - POSTGRES_PASSWORD=password
  command: >
    postgres -c wal_level=logical
             -c max_wal_senders=10
             -c max_replication_slots=10
```

**Запуск:**
```bash
cd ProjectTaskFlow/TaskFlow
docker-compose up -d
```

### Шаг 2: Подготовка базы данных

**Подключение к PostgreSQL:**
```bash
docker exec -it postgres psql -U postgres -d taskflow_db
```

**Создание outbox таблицы (что мы выполнили):**
```sql
CREATE TABLE outbox_events (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    event_data JSONB NOT NULL,
    created_at BIGINT DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000000
);
```

**Создание тестовой таблицы users:**
```sql
CREATE TABLE test_users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);
```

**Добавление тестовых данных (наши команды):**
```sql
-- Тестовые пользователи
INSERT INTO test_users (name, email) VALUES 
('John Doe', 'john@example.com'),
('Jane Smith', 'jane@example.com');

-- Тестовые события в outbox
INSERT INTO outbox_events (event_type, event_data) VALUES 
('USER_CREATED', '{"userId": 123, "name": "John Doe", "email": "john@example.com"}'),
('USER_UPDATED', '{"userId": 123, "name": "John Updated", "email": "john.new@example.com"}'),
('USER_PROFILE_VIEWED', '{"userId": 123, "viewerId": 456, "timestamp": "2025-06-25T10:30:00"}');
```

**Важно:** Outbox таблица должна быть в той же БД, что и бизнес-данные!

### Шаг 3: Настройка Debezium коннектора

**Создание файла debezium-postgres-connector.json (что мы использовали):**
```json
{
  "name": "postgres-outbox-connector",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "plugin.name": "pgoutput",
    "database.hostname": "postgres",
    "database.port": "5432",
    "database.user": "postgres",
    "database.password": "password",
    "database.dbname": "taskflow_db",
    "database.server.name": "taskflow",
    "table.include.list": "public.outbox_events,public.test_users",
    "publication.autocreate.mode": "filtered",
    "slot.name": "debezium_slot",
    "topic.prefix": "taskflow"
  }
}
```

**PowerShell команда для создания коннектора:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8083/connectors" -Method POST -ContentType "application/json" -Body (Get-Content "debezium-postgres-connector.json" -Raw)
```

**Проверка статуса коннектора (наши команды):**
```powershell
# Список коннекторов
Invoke-RestMethod -Uri "http://localhost:8083/connectors"

# Статус конкретного коннектора
Invoke-RestMethod -Uri "http://localhost:8083/connectors/postgres-outbox-connector/status"

# Удаление коннектора (если нужно пересоздать)
Invoke-RestMethod -Uri "http://localhost:8083/connectors/postgres-outbox-connector" -Method DELETE
```

**Проверка созданных топиков Kafka:**
```bash
docker exec kafka kafka-topics --list --bootstrap-server localhost:9092
# Результат: taskflow.public.outbox_events, taskflow.public.test_users
```

### Шаг 4: Создание Spring Boot CDC Consumer Service

**Структура проекта (что мы создали):**
```
CdcConsumerService/
├── build.gradle
├── src/main/java/com/abarigena/cdcconsumerservice/
│   ├── CdcConsumerServiceApplication.java
│   ├── config/
│   │   ├── KafkaConfig.java
│   │   └── OpenSearchConfig.java
│   ├── dto/
│   │   ├── DebeziumEvent.java
│   │   └── OutboxEventData.java
│   ├── consumer/
│   │   └── OutboxEventConsumer.java
│   ├── service/
│   │   └── OpenSearchSyncService.java
│   └── controller/
│       └── HealthController.java
└── src/main/resources/
    └── application.yml
```

**build.gradle (ключевые зависимости):**
```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.kafka:spring-kafka'
    implementation 'com.fasterxml.jackson.core:jackson-databind'
    implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310'
    
    // OpenSearch клиент (изначально был Elasticsearch, потом поменяли)
    implementation 'org.opensearch.client:opensearch-rest-client:2.10.0'
    implementation 'org.opensearch.client:opensearch-java:2.10.0'
    
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
}
```

**application.yml конфигурация:**
```yaml
server:
  port: 8084

spring:
  kafka:
    consumer:
      bootstrap-servers: localhost:29092
      group-id: cdc-consumer-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      auto-offset-reset: earliest

logging:
  level:
    com.abarigena.cdcconsumerservice: INFO
    org.apache.kafka: WARN
```

**Запуск и тестирование:**
```bash
cd ProjectTaskFlow/CdcConsumerService
./gradlew bootRun
```

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

### Проверка Debezium коннекторов (PowerShell команды):
```powershell
# Список всех коннекторов
Invoke-RestMethod -Uri "http://localhost:8083/connectors"

# Статус конкретного коннектора
Invoke-RestMethod -Uri "http://localhost:8083/connectors/postgres-outbox-connector/status"

# Конфигурация коннектора
Invoke-RestMethod -Uri "http://localhost:8083/connectors/postgres-outbox-connector/config"
```

### Мониторинг через Kafka Connect UI:
- Открыть http://localhost:8000
- Посмотреть статус коннекторов
- Проверить ошибки и метрики

### Проверка OpenSearch (наши команды):
```powershell
# Список индексов с количеством документов
Invoke-RestMethod -Uri "http://localhost:9200/_cat/indices?v"

# Поиск последних событий
$response = Invoke-RestMethod -Uri "http://localhost:9200/user-events-*/_search?sort=@timestamp:desc&size=5"
$response.hits.hits | ForEach-Object { 
    Write-Host "ID: $($_._id), Type: $($_._source.eventType), Time: $($_._source.'@timestamp')" 
}

# Количество событий по типам
Invoke-RestMethod -Uri "http://localhost:9200/user-events-*/_search?size=0" -Method POST -ContentType "application/json" -Body '{"aggs":{"event_types":{"terms":{"field":"eventType.keyword"}}}}'
```

### OpenSearch Dashboards (http://localhost:5601):
1. **Management** → **Index Patterns** → создать `user-events-*`
2. **Discover** → выбрать index pattern → посмотреть данные
3. **Dev Tools** → выполнить прямые запросы к OpenSearch

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

### Шаг 5: Тестирование системы

**Добавление новых событий (наши тесты):**
```sql
-- Подключаемся к БД
docker exec -it postgres psql -U postgres -d taskflow_db

-- Добавляем разные типы событий
INSERT INTO outbox_events (event_type, event_data) VALUES 
('USER_LOGIN', '{"ip": "192.168.1.1", "userId": 123, "loginTime": "2024-06-24T16:20:00"}'),
('ORDER_CREATED', '{"amount": 99.99, "orderId": 456, "currency": "USD", "customerId": 123}'),
('NOTIFICATION_SENT', '{"type": "EMAIL", "userId": 123, "subject": "Welcome!", "notificationId": 789}');
```

**Мониторинг Kafka Consumer (что мы видели в логах):**
```
🔥 Получено CDC событие из outbox: {"before":null,"after":{"id":47,"event_type":"USER_LOGIN"...
📋 Обрабатываем бизнес-событие:
   🆔 ID: 47
   📝 Тип: USER_LOGIN
   📄 Данные: {"ip": "192.168.1.1", "userId": 123, "loginTime": "2024-06-24T16:20:00"}
🔐 Обработка входа пользователя: {"ip": "192.168.1.1", "userId": 123...
📊 Событие синхронизировано с OpenSearch: userId=123, eventType=USER_LOGIN, index=user-events-2025-06
```

**Проверка данных в OpenSearch:**
```powershell
# Проверка индексов
Invoke-RestMethod -Uri "http://localhost:9200/_cat/indices?v"

# Поиск событий
$response = Invoke-RestMethod -Uri "http://localhost:9200/user-events-2025-06/_search?size=10&sort=@timestamp:desc"
$response.hits.hits | ForEach-Object { Write-Host "Event: $($_._source.eventType) at $($_._source.'@timestamp')" }
```

---

## 🚨 Проблемы и их решения (что мы встретили)

### 1. Коннектор исчезает
**Что произошло:** Коннектор создался, но потом пропал из списка
**Причина:** Ошибки в конфигурации или недоступность БД
**Как решили:** Пересоздали коннектор с правильной конфигурацией
```powershell
Invoke-RestMethod -Uri "http://localhost:8083/connectors/postgres-outbox-connector" -Method DELETE
Invoke-RestMethod -Uri "http://localhost:8083/connectors" -Method POST -ContentType "application/json" -Body (Get-Content "debezium-postgres-connector.json" -Raw)
```

### 2. Content-Type ошибка 406
**Что видели в логах:**
```
❌ Ошибка синхронизации с Elasticsearch: method [POST], host [http://localhost:9200], URI [/user-events-2025-06/_doc], status line [HTTP/1.1 406 Not Acceptable]
{"error":"Content-Type header [application/vnd.elasticsearch+json; compatible-with=8] is not supported","status":406}
```
**Причина:** Использование Elasticsearch клиента с OpenSearch
**Как решили:** Поменяли зависимости в build.gradle и переписали сервис для OpenSearch

### 3. Данные не видны в Dashboard
**Что произошло:** OpenSearch Dashboards показывал "No results match your search criteria"
**Причина:** Неправильный временной диапазон (события были от 10:49, а поиск до 10:30)
**Как решили:** Расширили временной диапазон на "Today" или использовали Dev Tools

### 4. Jackson парсинг ошибки
**Что встретили:** Ошибки парсинга LocalDateTime в JSON
**Как решили:** Добавили зависимость `jackson-datatype-jsr310` и аннотацию `@JsonIgnoreProperties(ignoreUnknown = true)`

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