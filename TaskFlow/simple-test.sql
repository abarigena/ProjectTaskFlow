INSERT INTO outbox_events (event_type, event_data) VALUES ('USER_LOGIN', '{"userId": 999, "action": "login_success"}');
SELECT 'Тестовое событие добавлено!' as result; 