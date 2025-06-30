-- Add password column if it doesn't exist
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='users' AND column_name='password') THEN
        ALTER TABLE users ADD COLUMN password VARCHAR(255);
    END IF;
END $$;

-- Add role column if it doesn't exist
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='users' AND column_name='role') THEN
        ALTER TABLE users ADD COLUMN role VARCHAR(50) DEFAULT 'USER' NOT NULL;
    END IF;
END $$;

-- Create index on email for faster authentication queries if it doesn't exist
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_users_email') THEN
        CREATE INDEX idx_users_email ON users(email);
    END IF;
END $$;

-- Update existing users with default values
UPDATE users SET role = 'USER' WHERE role IS NULL;
UPDATE users SET password = '$2a$10$N.zmdr9k7uOLQvQHbh6SqOeKPpUN3xt5o65F4a8FXUx0zKt8lc6M2' WHERE password IS NULL; -- default password 'password'

-- Make password required for new records if column exists and not already NOT NULL
DO $$ 
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name='users' AND column_name='password' AND is_nullable='YES') THEN
        ALTER TABLE users ALTER COLUMN password SET NOT NULL;
    END IF;
END $$; 