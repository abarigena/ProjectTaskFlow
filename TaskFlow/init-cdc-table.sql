-- Создаем тестовую таблицу для демонстрации CDC
CREATE TABLE IF NOT EXISTS outbox_events (
    id SERIAL PRIMARY KEY,
    event_type VARCHAR(255) NOT NULL,
    event_data JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Создаем таблицу для тестирования изменений
CREATE TABLE IF NOT EXISTS test_users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Добавляем несколько тестовых записей
INSERT INTO test_users (name, email) VALUES 
    ('Данил Галанов', 'danil.galanov@example.com'),
    ('Тест Пользователь', 'test.user@example.com');

-- Добавляем события в outbox
INSERT INTO outbox_events (event_type, event_data) VALUES 
    ('USER_CREATED', '{"userId": 1, "name": "Данил Галанов", "email": "danil.galanov@example.com"}'),
    ('USER_CREATED', '{"userId": 2, "name": "Тест Пользователь", "email": "test.user@example.com"}');

-- Проверяем что все создалось
SELECT 'Tables created successfully' as status;
SELECT COUNT(*) as users_count FROM test_users;
SELECT COUNT(*) as events_count FROM outbox_events; 