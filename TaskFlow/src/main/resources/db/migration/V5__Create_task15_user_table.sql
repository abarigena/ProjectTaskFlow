-- Task 15: JOOQ user table
CREATE TABLE task15_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('BORROWER', 'COLLECTOR', 'VERIFIER')),
    account_status VARCHAR(50) NOT NULL DEFAULT 'NOT_VERIFIED' CHECK (account_status IN ('NOT_VERIFIED', 'VERIFIED', 'BLOCKED')),
    date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Индексы для оптимизации поиска
CREATE INDEX idx_task15_user_username ON task15_user(username);
CREATE INDEX idx_task15_user_email ON task15_user(email);
CREATE INDEX idx_task15_user_role ON task15_user(role);
CREATE INDEX idx_task15_user_status ON task15_user(account_status);

-- Триггер для автоматического обновления date_updated
CREATE OR REPLACE FUNCTION update_task15_user_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.date_updated = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_task15_user_updated_at_trigger
    BEFORE UPDATE ON task15_user
    FOR EACH ROW
    EXECUTE FUNCTION update_task15_user_updated_at(); 