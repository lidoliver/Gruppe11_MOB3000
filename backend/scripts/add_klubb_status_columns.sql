-- Migration script to add missing columns to klubb table
-- Run this if you have an existing database

-- Add status column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'mydb'
        AND table_name = 'klubb'
        AND column_name = 'status'
    ) THEN
        ALTER TABLE mydb.klubb ADD COLUMN status VARCHAR(50) DEFAULT 'approved';
        RAISE NOTICE 'Added status column to mydb.klubb';
    ELSE
        RAISE NOTICE 'status column already exists in mydb.klubb';
    END IF;
END $$;

-- Add opprettet_dato column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'mydb'
        AND table_name = 'klubb'
        AND column_name = 'opprettet_dato'
    ) THEN
        ALTER TABLE mydb.klubb ADD COLUMN opprettet_dato TIMESTAMP DEFAULT NOW();
        RAISE NOTICE 'Added opprettet_dato column to mydb.klubb';
    ELSE
        RAISE NOTICE 'opprettet_dato column already exists in mydb.klubb';
    END IF;
END $$;

-- Update existing rows to have a default status if NULL
UPDATE mydb.klubb SET status = 'approved' WHERE status IS NULL;

-- Update existing rows to have a default date if NULL
UPDATE mydb.klubb SET opprettet_dato = NOW() WHERE opprettet_dato IS NULL;

SELECT 'Migration completed successfully' AS result;

