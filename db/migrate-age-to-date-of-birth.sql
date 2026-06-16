-- Migracja: age (integer) -> date_of_birth (date)
-- Uruchom na Neon (schemat monopoly) lub pozwol DatabaseSchemaPatch zrobic to przy starcie aplikacji.

SET search_path TO monopoly, public;

ALTER TABLE users ADD COLUMN IF NOT EXISTS date_of_birth date;

UPDATE users
SET date_of_birth = (CURRENT_DATE - (age * INTERVAL '1 year'))::date
WHERE date_of_birth IS NULL
  AND EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'monopoly'
      AND table_name = 'users'
      AND column_name = 'age'
  );

UPDATE users
SET date_of_birth = (CURRENT_DATE - INTERVAL '25 years')::date
WHERE date_of_birth IS NULL;

ALTER TABLE users ALTER COLUMN date_of_birth SET NOT NULL;
ALTER TABLE users DROP COLUMN IF EXISTS age;
