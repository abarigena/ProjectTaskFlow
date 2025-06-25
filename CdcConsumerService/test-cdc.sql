-- Тестовые события для проверки CDC
INSERT INTO outbox_events (event_type, event_data) VALUES 
    ('USER_LOGIN', '{"userId": 123, "loginTime": "2024-06-24T16:20:00", "ip": "192.168.1.1"}');

INSERT INTO outbox_events (event_type, event_data) VALUES 
    ('ORDER_CREATED', '{"orderId": 456, "customerId": 123, "amount": 99.99, "currency": "USD"}');

INSERT INTO outbox_events (event_type, event_data) VALUES 
    ('NOTIFICATION_SENT', '{"notificationId": 789, "userId": 123, "type": "EMAIL", "subject": "Welcome!"}');

SELECT 'Тестовые CDC события добавлены!' as result; 