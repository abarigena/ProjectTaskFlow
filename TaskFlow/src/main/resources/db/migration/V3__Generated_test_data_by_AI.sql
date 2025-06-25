-- Миграция V3: Генерация тестовых данных с помощью ИИ
-- Создает 20 пользователей, 5 проектов, множество задач и комментариев для тестирования

-- ===== ПОЛЬЗОВАТЕЛИ (20 штук) =====
INSERT INTO users (first_name, last_name, email, active, created_at, updated_at) VALUES
('Алексей', 'Иванов', 'alex.ivanov@example.com', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Мария', 'Петрова', 'maria.petrova@example.com', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Дмитрий', 'Сидоров', 'dmitry.sidorov@example.com', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Анна', 'Козлова', 'anna.kozlova@example.com', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Сергей', 'Морозов', 'sergey.morozov@example.com', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Екатерина', 'Новикова', 'ekaterina.novikova@example.com', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Павел', 'Волков', 'pavel.volkov@example.com', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Ольга', 'Соколова', 'olga.sokolova@example.com', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Андрей', 'Лебедев', 'andrey.lebedev@example.com', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Татьяна', 'Семенова', 'tatyana.semenova@example.com', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Михаил', 'Егоров', 'mikhail.egorov@example.com', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Светлана', 'Кузнецова', 'svetlana.kuznetsova@example.com', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Виктор', 'Попов', 'viktor.popov@example.com', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Наталья', 'Васильева', 'natalya.vasileva@example.com', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Игорь', 'Федоров', 'igor.fedorov@example.com', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Юлия', 'Михайлова', 'yulia.mikhailova@example.com', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Константин', 'Беляев', 'konstantin.belyaev@example.com', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Валентина', 'Тарасова', 'valentina.tarasova@example.com', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Роман', 'Белов', 'roman.belov@example.com', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Елена', 'Орлова', 'elena.orlova@example.com', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ===== ПРОЕКТЫ (5 штук) =====
-- Используем подзапросы для получения реальных ID пользователей
INSERT INTO projects (name, description, status, owner_id, created_at, updated_at) VALUES
('E-Commerce Platform', 'Разработка интернет-магазина с полным функционалом: каталог товаров, корзина, оплата, админ-панель', 'ACTIVE', (SELECT id FROM users WHERE email = 'alex.ivanov@example.com'), CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP),
('Mobile Banking App', 'Мобильное приложение для банковских операций: переводы, платежи, управление картами', 'ACTIVE', (SELECT id FROM users WHERE email = 'maria.petrova@example.com'), CURRENT_TIMESTAMP - INTERVAL '45 days', CURRENT_TIMESTAMP),
('CRM System', 'Система управления взаимоотношениями с клиентами для среднего бизнеса', 'COMPLETED', (SELECT id FROM users WHERE email = 'dmitry.sidorov@example.com'), CURRENT_TIMESTAMP - INTERVAL '90 days', CURRENT_TIMESTAMP - INTERVAL '10 days'),
('Data Analytics Dashboard', 'Панель аналитики данных с визуализацией и отчетностью в реальном времени', 'ACTIVE', (SELECT id FROM users WHERE email = 'anna.kozlova@example.com'), CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP),
('Legacy System Migration', 'Миграция устаревшей системы на современные технологии', 'ARCHIVED', (SELECT id FROM users WHERE email = 'sergey.morozov@example.com'), CURRENT_TIMESTAMP - INTERVAL '120 days', CURRENT_TIMESTAMP - INTERVAL '60 days');

-- ===== СВЯЗИ ПОЛЬЗОВАТЕЛЕЙ С ПРОЕКТАМИ =====
-- Используем подзапросы для получения реальных ID проектов и пользователей
INSERT INTO project_users (project_id, user_id) VALUES
-- E-Commerce Platform (команда из 8 человек)
((SELECT id FROM projects WHERE name = 'E-Commerce Platform'), (SELECT id FROM users WHERE email = 'alex.ivanov@example.com')),
((SELECT id FROM projects WHERE name = 'E-Commerce Platform'), (SELECT id FROM users WHERE email = 'maria.petrova@example.com')),
((SELECT id FROM projects WHERE name = 'E-Commerce Platform'), (SELECT id FROM users WHERE email = 'ekaterina.novikova@example.com')),
((SELECT id FROM projects WHERE name = 'E-Commerce Platform'), (SELECT id FROM users WHERE email = 'pavel.volkov@example.com')),
((SELECT id FROM projects WHERE name = 'E-Commerce Platform'), (SELECT id FROM users WHERE email = 'mikhail.egorov@example.com')),
((SELECT id FROM projects WHERE name = 'E-Commerce Platform'), (SELECT id FROM users WHERE email = 'svetlana.kuznetsova@example.com')),
((SELECT id FROM projects WHERE name = 'E-Commerce Platform'), (SELECT id FROM users WHERE email = 'yulia.mikhailova@example.com')),
((SELECT id FROM projects WHERE name = 'E-Commerce Platform'), (SELECT id FROM users WHERE email = 'konstantin.belyaev@example.com')),

-- Mobile Banking App (команда из 6 человек)
((SELECT id FROM projects WHERE name = 'Mobile Banking App'), (SELECT id FROM users WHERE email = 'maria.petrova@example.com')),
((SELECT id FROM projects WHERE name = 'Mobile Banking App'), (SELECT id FROM users WHERE email = 'dmitry.sidorov@example.com')),
((SELECT id FROM projects WHERE name = 'Mobile Banking App'), (SELECT id FROM users WHERE email = 'olga.sokolova@example.com')),
((SELECT id FROM projects WHERE name = 'Mobile Banking App'), (SELECT id FROM users WHERE email = 'andrey.lebedev@example.com')),
((SELECT id FROM projects WHERE name = 'Mobile Banking App'), (SELECT id FROM users WHERE email = 'viktor.popov@example.com')),
((SELECT id FROM projects WHERE name = 'Mobile Banking App'), (SELECT id FROM users WHERE email = 'valentina.tarasova@example.com')),

-- CRM System (команда из 5 человек)
((SELECT id FROM projects WHERE name = 'CRM System'), (SELECT id FROM users WHERE email = 'dmitry.sidorov@example.com')),
((SELECT id FROM projects WHERE name = 'CRM System'), (SELECT id FROM users WHERE email = 'anna.kozlova@example.com')),
((SELECT id FROM projects WHERE name = 'CRM System'), (SELECT id FROM users WHERE email = 'natalya.vasileva@example.com')),
((SELECT id FROM projects WHERE name = 'CRM System'), (SELECT id FROM users WHERE email = 'igor.fedorov@example.com')),
((SELECT id FROM projects WHERE name = 'CRM System'), (SELECT id FROM users WHERE email = 'roman.belov@example.com')),

-- Data Analytics Dashboard (команда из 7 человек)
((SELECT id FROM projects WHERE name = 'Data Analytics Dashboard'), (SELECT id FROM users WHERE email = 'anna.kozlova@example.com')),
((SELECT id FROM projects WHERE name = 'Data Analytics Dashboard'), (SELECT id FROM users WHERE email = 'sergey.morozov@example.com')),
((SELECT id FROM projects WHERE name = 'Data Analytics Dashboard'), (SELECT id FROM users WHERE email = 'tatyana.semenova@example.com')),
((SELECT id FROM projects WHERE name = 'Data Analytics Dashboard'), (SELECT id FROM users WHERE email = 'mikhail.egorov@example.com')),
((SELECT id FROM projects WHERE name = 'Data Analytics Dashboard'), (SELECT id FROM users WHERE email = 'yulia.mikhailova@example.com')),
((SELECT id FROM projects WHERE name = 'Data Analytics Dashboard'), (SELECT id FROM users WHERE email = 'valentina.tarasova@example.com')),
((SELECT id FROM projects WHERE name = 'Data Analytics Dashboard'), (SELECT id FROM users WHERE email = 'elena.orlova@example.com')),

-- Legacy System Migration (команда из 4 человека)
((SELECT id FROM projects WHERE name = 'Legacy System Migration'), (SELECT id FROM users WHERE email = 'sergey.morozov@example.com')),
((SELECT id FROM projects WHERE name = 'Legacy System Migration'), (SELECT id FROM users WHERE email = 'svetlana.kuznetsova@example.com')),
((SELECT id FROM projects WHERE name = 'Legacy System Migration'), (SELECT id FROM users WHERE email = 'viktor.popov@example.com')),
((SELECT id FROM projects WHERE name = 'Legacy System Migration'), (SELECT id FROM users WHERE email = 'roman.belov@example.com'));

-- ===== ЗАДАЧИ (множество задач для каждого проекта) =====

-- Задачи для E-Commerce Platform
INSERT INTO tasks (title, description, status, priority, deadline, assigned_user_id, project_id, created_at, updated_at) VALUES
('Создать архитектуру базы данных', 'Спроектировать и создать схему базы данных для интернет-магазина', 'DONE', 'HIGH', CURRENT_TIMESTAMP - INTERVAL '25 days', (SELECT id FROM users WHERE email = 'alex.ivanov@example.com'), (SELECT id FROM projects WHERE name = 'E-Commerce Platform'), CURRENT_TIMESTAMP - INTERVAL '28 days', CURRENT_TIMESTAMP - INTERVAL '25 days'),
('Разработать API для каталога товаров', 'Создать REST API для управления каталогом товаров', 'DONE', 'HIGH', CURRENT_TIMESTAMP - INTERVAL '20 days', (SELECT id FROM users WHERE email = 'maria.petrova@example.com'), (SELECT id FROM projects WHERE name = 'E-Commerce Platform'), CURRENT_TIMESTAMP - INTERVAL '25 days', CURRENT_TIMESTAMP - INTERVAL '20 days'),
('Создать страницы товаров', 'Разработать фронтенд страницы для отображения товаров', 'IN_PROGRESS', 'MEDIUM', CURRENT_TIMESTAMP + INTERVAL '5 days', (SELECT id FROM users WHERE email = 'ekaterina.novikova@example.com'), (SELECT id FROM projects WHERE name = 'E-Commerce Platform'), CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP),
('Интегрировать платежную систему', 'Подключить и настроить платежную систему', 'TODO', 'HIGH', CURRENT_TIMESTAMP + INTERVAL '10 days', (SELECT id FROM users WHERE email = 'pavel.volkov@example.com'), (SELECT id FROM projects WHERE name = 'E-Commerce Platform'), CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP),
('Создать корзину покупок', 'Разработать функционал корзины покупок', 'IN_PROGRESS', 'MEDIUM', CURRENT_TIMESTAMP + INTERVAL '7 days', (SELECT id FROM users WHERE email = 'mikhail.egorov@example.com'), (SELECT id FROM projects WHERE name = 'E-Commerce Platform'), CURRENT_TIMESTAMP - INTERVAL '12 days', CURRENT_TIMESTAMP),
('Разработать админ-панель', 'Создать административную панель для управления магазином', 'TODO', 'MEDIUM', CURRENT_TIMESTAMP + INTERVAL '15 days', (SELECT id FROM users WHERE email = 'svetlana.kuznetsova@example.com'), (SELECT id FROM projects WHERE name = 'E-Commerce Platform'), CURRENT_TIMESTAMP - INTERVAL '8 days', CURRENT_TIMESTAMP),
('Настроить CI/CD пипелайн', 'Настроить автоматическое тестирование и деплой', 'TODO', 'LOW', CURRENT_TIMESTAMP + INTERVAL '20 days', (SELECT id FROM users WHERE email = 'yulia.mikhailova@example.com'), (SELECT id FROM projects WHERE name = 'E-Commerce Platform'), CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP),
('Провести нагрузочное тестирование', 'Протестировать производительность системы', 'TODO', 'MEDIUM', CURRENT_TIMESTAMP + INTERVAL '25 days', (SELECT id FROM users WHERE email = 'konstantin.belyaev@example.com'), (SELECT id FROM projects WHERE name = 'E-Commerce Platform'), CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP),

-- Задачи для Mobile Banking App
('Создать дизайн приложения', 'Разработать UI/UX дизайн мобильного приложения', 'DONE', 'HIGH', CURRENT_TIMESTAMP - INTERVAL '35 days', (SELECT id FROM users WHERE email = 'olga.sokolova@example.com'), (SELECT id FROM projects WHERE name = 'Mobile Banking App'), CURRENT_TIMESTAMP - INTERVAL '40 days', CURRENT_TIMESTAMP - INTERVAL '35 days'),
('Разработать авторизацию', 'Создать систему безопасной авторизации пользователей', 'DONE', 'HIGH', CURRENT_TIMESTAMP - INTERVAL '30 days', (SELECT id FROM users WHERE email = 'dmitry.sidorov@example.com'), (SELECT id FROM projects WHERE name = 'Mobile Banking App'), CURRENT_TIMESTAMP - INTERVAL '35 days', CURRENT_TIMESTAMP - INTERVAL '30 days'),
('Создать экраны переводов', 'Разработать интерфейс для переводов между счетами', 'IN_PROGRESS', 'HIGH', CURRENT_TIMESTAMP + INTERVAL '3 days', (SELECT id FROM users WHERE email = 'andrey.lebedev@example.com'), (SELECT id FROM projects WHERE name = 'Mobile Banking App'), CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP),
('Интегрировать с банковскими API', 'Подключить приложение к банковским системам', 'IN_PROGRESS', 'HIGH', CURRENT_TIMESTAMP + INTERVAL '8 days', (SELECT id FROM users WHERE email = 'viktor.popov@example.com'), (SELECT id FROM projects WHERE name = 'Mobile Banking App'), CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP),
('Добавить управление картами', 'Создать функционал управления банковскими картами', 'TODO', 'MEDIUM', CURRENT_TIMESTAMP + INTERVAL '12 days', (SELECT id FROM users WHERE email = 'valentina.tarasova@example.com'), (SELECT id FROM projects WHERE name = 'Mobile Banking App'), CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP),
('Провести security-аудит', 'Проверить безопасность приложения', 'TODO', 'HIGH', CURRENT_TIMESTAMP + INTERVAL '20 days', (SELECT id FROM users WHERE email = 'maria.petrova@example.com'), (SELECT id FROM projects WHERE name = 'Mobile Banking App'), CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP),

-- Задачи для CRM System (завершенный проект)
('Анализ требований заказчика', 'Собрать и проанализировать требования к CRM системе', 'DONE', 'HIGH', CURRENT_TIMESTAMP - INTERVAL '85 days', (SELECT id FROM users WHERE email = 'dmitry.sidorov@example.com'), (SELECT id FROM projects WHERE name = 'CRM System'), CURRENT_TIMESTAMP - INTERVAL '88 days', CURRENT_TIMESTAMP - INTERVAL '85 days'),
('Создать базу данных CRM', 'Спроектировать и создать базу данных для CRM', 'DONE', 'HIGH', CURRENT_TIMESTAMP - INTERVAL '75 days', (SELECT id FROM users WHERE email = 'anna.kozlova@example.com'), (SELECT id FROM projects WHERE name = 'CRM System'), CURRENT_TIMESTAMP - INTERVAL '80 days', CURRENT_TIMESTAMP - INTERVAL '75 days'),
('Разработать модуль клиентов', 'Создать модуль управления клиентами', 'DONE', 'HIGH', CURRENT_TIMESTAMP - INTERVAL '65 days', (SELECT id FROM users WHERE email = 'natalya.vasileva@example.com'), (SELECT id FROM projects WHERE name = 'CRM System'), CURRENT_TIMESTAMP - INTERVAL '70 days', CURRENT_TIMESTAMP - INTERVAL '65 days'),
('Создать модуль продаж', 'Разработать модуль управления продажами', 'DONE', 'HIGH', CURRENT_TIMESTAMP - INTERVAL '55 days', (SELECT id FROM users WHERE email = 'igor.fedorov@example.com'), (SELECT id FROM projects WHERE name = 'CRM System'), CURRENT_TIMESTAMP - INTERVAL '60 days', CURRENT_TIMESTAMP - INTERVAL '55 days'),
('Интегрировать с почтой', 'Подключить email-интеграцию', 'DONE', 'MEDIUM', CURRENT_TIMESTAMP - INTERVAL '45 days', (SELECT id FROM users WHERE email = 'roman.belov@example.com'), (SELECT id FROM projects WHERE name = 'CRM System'), CURRENT_TIMESTAMP - INTERVAL '50 days', CURRENT_TIMESTAMP - INTERVAL '45 days'),
('Создать отчеты и аналитику', 'Разработать систему отчетности', 'DONE', 'MEDIUM', CURRENT_TIMESTAMP - INTERVAL '35 days', (SELECT id FROM users WHERE email = 'dmitry.sidorov@example.com'), (SELECT id FROM projects WHERE name = 'CRM System'), CURRENT_TIMESTAMP - INTERVAL '40 days', CURRENT_TIMESTAMP - INTERVAL '35 days'),
('Провести приемочное тестирование', 'Финальное тестирование с заказчиком', 'DONE', 'HIGH', CURRENT_TIMESTAMP - INTERVAL '15 days', (SELECT id FROM users WHERE email = 'anna.kozlova@example.com'), (SELECT id FROM projects WHERE name = 'CRM System'), CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP - INTERVAL '15 days'),

-- Задачи для Data Analytics Dashboard
('Настроить сбор данных', 'Создать систему сбора данных из различных источников', 'DONE', 'HIGH', CURRENT_TIMESTAMP - INTERVAL '15 days', (SELECT id FROM users WHERE email = 'sergey.morozov@example.com'), (SELECT id FROM projects WHERE name = 'Data Analytics Dashboard'), CURRENT_TIMESTAMP - INTERVAL '18 days', CURRENT_TIMESTAMP - INTERVAL '15 days'),
('Разработать дашборд интерфейс', 'Создать интерактивный дашборд для визуализации', 'IN_PROGRESS', 'HIGH', CURRENT_TIMESTAMP + INTERVAL '5 days', (SELECT id FROM users WHERE email = 'tatyana.semenova@example.com'), (SELECT id FROM projects WHERE name = 'Data Analytics Dashboard'), CURRENT_TIMESTAMP - INTERVAL '12 days', CURRENT_TIMESTAMP),
('Создать систему алертов', 'Разработать систему уведомлений о критических метриках', 'TODO', 'MEDIUM', CURRENT_TIMESTAMP + INTERVAL '10 days', (SELECT id FROM users WHERE email = 'mikhail.egorov@example.com'), (SELECT id FROM projects WHERE name = 'Data Analytics Dashboard'), CURRENT_TIMESTAMP - INTERVAL '8 days', CURRENT_TIMESTAMP),
('Оптимизировать запросы к БД', 'Улучшить производительность запросов', 'IN_PROGRESS', 'MEDIUM', CURRENT_TIMESTAMP + INTERVAL '7 days', (SELECT id FROM users WHERE email = 'yulia.mikhailova@example.com'), (SELECT id FROM projects WHERE name = 'Data Analytics Dashboard'), CURRENT_TIMESTAMP - INTERVAL '6 days', CURRENT_TIMESTAMP),
('Добавить экспорт данных', 'Создать функционал экспорта отчетов', 'TODO', 'LOW', CURRENT_TIMESTAMP + INTERVAL '15 days', (SELECT id FROM users WHERE email = 'valentina.tarasova@example.com'), (SELECT id FROM projects WHERE name = 'Data Analytics Dashboard'), CURRENT_TIMESTAMP - INTERVAL '4 days', CURRENT_TIMESTAMP),
('Создать пользовательские роли', 'Разработать систему ролей и доступов', 'TODO', 'MEDIUM', CURRENT_TIMESTAMP + INTERVAL '12 days', (SELECT id FROM users WHERE email = 'elena.orlova@example.com'), (SELECT id FROM projects WHERE name = 'Data Analytics Dashboard'), CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP),

-- Задачи для Legacy System Migration (архивный проект)
('Анализ устаревшей системы', 'Провести полный анализ текущей системы', 'DONE', 'HIGH', CURRENT_TIMESTAMP - INTERVAL '115 days', (SELECT id FROM users WHERE email = 'sergey.morozov@example.com'), (SELECT id FROM projects WHERE name = 'Legacy System Migration'), CURRENT_TIMESTAMP - INTERVAL '118 days', CURRENT_TIMESTAMP - INTERVAL '115 days'),
('Создать план миграции', 'Разработать детальный план переноса данных', 'DONE', 'HIGH', CURRENT_TIMESTAMP - INTERVAL '105 days', (SELECT id FROM users WHERE email = 'svetlana.kuznetsova@example.com'), (SELECT id FROM projects WHERE name = 'Legacy System Migration'), CURRENT_TIMESTAMP - INTERVAL '110 days', CURRENT_TIMESTAMP - INTERVAL '105 days'),
('Перенести пользовательские данные', 'Мигрировать данные пользователей в новую систему', 'DONE', 'HIGH', CURRENT_TIMESTAMP - INTERVAL '95 days', (SELECT id FROM users WHERE email = 'viktor.popov@example.com'), (SELECT id FROM projects WHERE name = 'Legacy System Migration'), CURRENT_TIMESTAMP - INTERVAL '100 days', CURRENT_TIMESTAMP - INTERVAL '95 days'),
('Обновить бизнес-логику', 'Адаптировать бизнес-процессы под новую систему', 'DONE', 'MEDIUM', CURRENT_TIMESTAMP - INTERVAL '85 days', (SELECT id FROM users WHERE email = 'roman.belov@example.com'), (SELECT id FROM projects WHERE name = 'Legacy System Migration'), CURRENT_TIMESTAMP - INTERVAL '90 days', CURRENT_TIMESTAMP - INTERVAL '85 days'),
('Провести финальное тестирование', 'Комплексное тестирование новой системы', 'DONE', 'HIGH', CURRENT_TIMESTAMP - INTERVAL '70 days', (SELECT id FROM users WHERE email = 'sergey.morozov@example.com'), (SELECT id FROM projects WHERE name = 'Legacy System Migration'), CURRENT_TIMESTAMP - INTERVAL '75 days', CURRENT_TIMESTAMP - INTERVAL '70 days');

-- ===== КОММЕНТАРИИ К ЗАДАЧАМ =====
INSERT INTO comments (context, task_id, user_id, created_at, updated_at) VALUES
-- Комментарии к задачам E-Commerce Platform
('Схема БД готова, добавил индексы для оптимизации поиска товаров', (SELECT id FROM tasks WHERE title = 'Создать архитектуру базы данных'), (SELECT id FROM users WHERE email = 'alex.ivanov@example.com'), CURRENT_TIMESTAMP - INTERVAL '25 days', CURRENT_TIMESTAMP - INTERVAL '25 days'),
('Отлично! Можем переходить к следующему этапу', (SELECT id FROM tasks WHERE title = 'Создать архитектуру базы данных'), (SELECT id FROM users WHERE email = 'maria.petrova@example.com'), CURRENT_TIMESTAMP - INTERVAL '24 days', CURRENT_TIMESTAMP - INTERVAL '24 days'),
('API готово, добавил пагинацию и фильтрацию', (SELECT id FROM tasks WHERE title = 'Разработать API для каталога товаров'), (SELECT id FROM users WHERE email = 'maria.petrova@example.com'), CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP - INTERVAL '20 days'),
('Проверил API, все работает отлично!', (SELECT id FROM tasks WHERE title = 'Разработать API для каталога товаров'), (SELECT id FROM users WHERE email = 'ekaterina.novikova@example.com'), CURRENT_TIMESTAMP - INTERVAL '19 days', CURRENT_TIMESTAMP - INTERVAL '19 days'),
('Работаю над адаптивным дизайном страниц', (SELECT id FROM tasks WHERE title = 'Создать страницы товаров'), (SELECT id FROM users WHERE email = 'ekaterina.novikova@example.com'), CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '2 days'),
('Нужна помощь с интеграцией Stripe API', (SELECT id FROM tasks WHERE title = 'Интегрировать платежную систему'), (SELECT id FROM users WHERE email = 'pavel.volkov@example.com'), CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '1 day'),
('Могу помочь, у меня есть опыт с платежными системами', (SELECT id FROM tasks WHERE title = 'Интегрировать платежную систему'), (SELECT id FROM users WHERE email = 'maria.petrova@example.com'), CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '1 day'),
('Добавил функции добавления/удаления товаров из корзины', (SELECT id FROM tasks WHERE title = 'Создать корзину покупок'), (SELECT id FROM users WHERE email = 'mikhail.egorov@example.com'), CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '3 days'),

-- Комментарии к задачам Mobile Banking App
('Дизайн согласован с командой безопасности', (SELECT id FROM tasks WHERE title = 'Создать дизайн приложения'), (SELECT id FROM users WHERE email = 'olga.sokolova@example.com'), CURRENT_TIMESTAMP - INTERVAL '35 days', CURRENT_TIMESTAMP - INTERVAL '35 days'),
('Добавил двухфакторную аутентификацию', (SELECT id FROM tasks WHERE title = 'Разработать авторизацию'), (SELECT id FROM users WHERE email = 'dmitry.sidorov@example.com'), CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '30 days'),
('Отлично! Безопасность на высоком уровне', (SELECT id FROM tasks WHERE title = 'Разработать авторизацию'), (SELECT id FROM users WHERE email = 'maria.petrova@example.com'), CURRENT_TIMESTAMP - INTERVAL '29 days', CURRENT_TIMESTAMP - INTERVAL '29 days'),
('Реализовал валидацию переводов в реальном времени', (SELECT id FROM tasks WHERE title = 'Создать экраны переводов'), (SELECT id FROM users WHERE email = 'andrey.lebedev@example.com'), CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '1 day'),
('API банка временно недоступно, жду ответа от техподдержки', (SELECT id FROM tasks WHERE title = 'Интегрировать с банковскими API'), (SELECT id FROM users WHERE email = 'viktor.popov@example.com'), CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '2 days'),

-- Комментарии к завершенному CRM проекту
('Требования собраны и структурированы', (SELECT id FROM tasks WHERE title = 'Анализ требований заказчика'), (SELECT id FROM users WHERE email = 'dmitry.sidorov@example.com'), CURRENT_TIMESTAMP - INTERVAL '85 days', CURRENT_TIMESTAMP - INTERVAL '85 days'),
('База данных оптимизирована для быстрого поиска клиентов', (SELECT id FROM tasks WHERE title = 'Создать базу данных CRM'), (SELECT id FROM users WHERE email = 'anna.kozlova@example.com'), CURRENT_TIMESTAMP - INTERVAL '75 days', CURRENT_TIMESTAMP - INTERVAL '75 days'),
('Модуль клиентов готов, добавил экспорт в Excel', (SELECT id FROM tasks WHERE title = 'Разработать модуль клиентов'), (SELECT id FROM users WHERE email = 'natalya.vasileva@example.com'), CURRENT_TIMESTAMP - INTERVAL '65 days', CURRENT_TIMESTAMP - INTERVAL '65 days'),
('Интеграция с Outlook работает стабильно', (SELECT id FROM tasks WHERE title = 'Интегрировать с почтой'), (SELECT id FROM users WHERE email = 'roman.belov@example.com'), CURRENT_TIMESTAMP - INTERVAL '45 days', CURRENT_TIMESTAMP - INTERVAL '45 days'),
('Проект успешно сдан заказчику!', (SELECT id FROM tasks WHERE title = 'Провести приемочное тестирование'), (SELECT id FROM users WHERE email = 'dmitry.sidorov@example.com'), CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP - INTERVAL '15 days'),

-- Комментарии к Data Analytics Dashboard
('Подключил источники данных: PostgreSQL, MongoDB, внешние API', (SELECT id FROM tasks WHERE title = 'Настроить сбор данных'), (SELECT id FROM users WHERE email = 'sergey.morozov@example.com'), CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP - INTERVAL '15 days'),
('Дашборд выглядит отлично! Добавил интерактивные графики', (SELECT id FROM tasks WHERE title = 'Разработать дашборд интерфейс'), (SELECT id FROM users WHERE email = 'tatyana.semenova@example.com'), CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '2 days'),
('Нужно обсудить алгоритмы для алертов', (SELECT id FROM tasks WHERE title = 'Создать систему алертов'), (SELECT id FROM users WHERE email = 'mikhail.egorov@example.com'), CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '1 day'),
('Оптимизировал основные запросы, производительность выросла в 3 раза', (SELECT id FROM tasks WHERE title = 'Оптимизировать запросы к БД'), (SELECT id FROM users WHERE email = 'yulia.mikhailova@example.com'), CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '1 day'),

-- Комментарии к архивному проекту миграции
('Анализ завершен, система очень устарела', (SELECT id FROM tasks WHERE title = 'Анализ устаревшей системы'), (SELECT id FROM users WHERE email = 'sergey.morozov@example.com'), CURRENT_TIMESTAMP - INTERVAL '115 days', CURRENT_TIMESTAMP - INTERVAL '115 days'),
('План миграции утвержден руководством', (SELECT id FROM tasks WHERE title = 'Создать план миграции'), (SELECT id FROM users WHERE email = 'svetlana.kuznetsova@example.com'), CURRENT_TIMESTAMP - INTERVAL '105 days', CURRENT_TIMESTAMP - INTERVAL '105 days'),
('Данные успешно перенесены без потерь', (SELECT id FROM tasks WHERE title = 'Перенести пользовательские данные'), (SELECT id FROM users WHERE email = 'viktor.popov@example.com'), CURRENT_TIMESTAMP - INTERVAL '95 days', CURRENT_TIMESTAMP - INTERVAL '95 days'),
('Проект завершен успешно, система работает стабильно', (SELECT id FROM tasks WHERE title = 'Провести финальное тестирование'), (SELECT id FROM users WHERE email = 'sergey.morozov@example.com'), CURRENT_TIMESTAMP - INTERVAL '70 days', CURRENT_TIMESTAMP - INTERVAL '70 days');