-- Добавляем новое событие для демонстрации CDC
INSERT INTO outbox_events (event_type, event_data) VALUES 
    ('USER_UPDATED', '{"userId": 1, "action": "profile_updated", "timestamp": "2024-06-24T15:30:00"}');

-- Добавляем нового пользователя
INSERT INTO test_users (name, email) VALUES 
    ('Новый Пользователь', 'new.user@example.com');

-- Обновляем существующего пользователя
UPDATE test_users SET name = 'Данил Галанов (Обновлено)' WHERE id = 1;

-- Добавляем еще одно событие
INSERT INTO outbox_events (event_type, event_data) VALUES 
    ('USER_PROFILE_VIEWED', '{"userId": 2, "viewedBy": 1, "timestamp": "2024-06-24T15:31:00"}');

SELECT 'Тестовые данные добавлены успешно!' as result; 