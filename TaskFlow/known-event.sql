INSERT INTO outbox_events (event_type, event_data) VALUES ('USER_CREATED', '{"userId": 777, "name": "Test User", "email": "test@example.com"}');
SELECT 'USER_CREATED событие добавлено!' as result; 