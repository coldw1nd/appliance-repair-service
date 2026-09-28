INSERT INTO customers (full_name, phone, email, address) VALUES
('Иванов Иван Иванович', '+7-900-111-11-11', 'ivanov@example.com', 'г. Москва, ул. Лесная, д. 10'),
('Петрова Анна Сергеевна', '+7-900-222-22-22', 'petrova@example.com', 'г. Москва, ул. Садовая, д. 25'),
('Сидоров Алексей Павлович', '+7-900-333-33-33', 'sidorov@example.com', 'г. Москва, ул. Новая, д. 7'),
('Кузнецова Мария Олеговна', '+7-900-444-44-44', 'kuznetsova@example.com', 'г. Москва, ул. Центральная, д. 18'),
('Смирнов Дмитрий Викторович', '+7-900-555-55-55', 'smirnov@example.com', 'г. Москва, ул. Полевая, д. 42')
ON CONFLICT (email) DO NOTHING;

INSERT INTO repair_requests (customer_id, appliance_type, appliance_brand, issue_title, issue_description, status, priority, estimated_cost, planned_date, urgent)
SELECT c.id, v.appliance_type, v.appliance_brand, v.issue_title, v.issue_description, v.status, v.priority, v.estimated_cost, v.planned_date, v.urgent
FROM (VALUES
    ('ivanov@example.com', 'Стиральная машина', 'Bosch', 'Не сливает воду', 'После стирки вода остается в баке.', 'NEW', 'HIGH', 4500.00, CURRENT_DATE + 1, TRUE),
    ('petrova@example.com', 'Холодильник', 'LG', 'Не охлаждает', 'Температура в камере повышается.', 'DIAGNOSING', 'URGENT', 7000.00, CURRENT_DATE + 2, TRUE),
    ('sidorov@example.com', 'Пылесос', 'Samsung', 'Потеря мощности', 'Пылесос работает, но тяга значительно снизилась.', 'REPAIRING', 'MEDIUM', 2500.00, CURRENT_DATE + 3, FALSE),
    ('kuznetsova@example.com', 'Посудомоечная машина', 'Electrolux', 'Не запускается', 'После нажатия кнопки запуск не происходит.', 'COMPLETED', 'HIGH', 6000.00, CURRENT_DATE - 2, FALSE),
    ('smirnov@example.com', 'Микроволновая печь', 'Panasonic', 'Не нагревает', 'Таймер работает, но еда не нагревается.', 'CANCELLED', 'LOW', 1800.00, CURRENT_DATE + 4, FALSE),
    ('ivanov@example.com', 'Телевизор', 'Sony', 'Нет изображения', 'Есть звук, изображение отсутствует.', 'NEW', 'MEDIUM', 5200.00, CURRENT_DATE + 5, FALSE),
    ('petrova@example.com', 'Электрическая плита', 'Gorenje', 'Не включается конфорка', 'Одна конфорка не нагревается.', 'DIAGNOSING', 'MEDIUM', 3200.00, CURRENT_DATE + 6, FALSE),
    ('sidorov@example.com', 'Сушильная машина', 'Beko', 'Не сушит', 'Барабан работает, но вещи остаются влажными.', 'REPAIRING', 'HIGH', 4800.00, CURRENT_DATE + 7, FALSE),
    ('kuznetsova@example.com', 'Кофемашина', 'Philips', 'Течь воды', 'Вода появляется под корпусом.', 'NEW', 'HIGH', 3700.00, CURRENT_DATE + 8, TRUE),
    ('smirnov@example.com', 'Духовой шкаф', 'Haier', 'Ошибка нагрева', 'Появляется ошибка при включении нагрева.', 'COMPLETED', 'MEDIUM', 6100.00, CURRENT_DATE - 1, FALSE)
) AS v(email, appliance_type, appliance_brand, issue_title, issue_description, status, priority, estimated_cost, planned_date, urgent)
JOIN customers c ON c.email = v.email
WHERE NOT EXISTS (SELECT 1 FROM repair_requests r WHERE r.issue_title = v.issue_title AND r.customer_id = c.id);
