-- =====================================================================
--  Migracja: balance (PLN) -> coins (waluta metagry)
--  Powod: encja User.coins (int) zastapila User.balance (numeric).
--  Hibernate (ddl-auto=update) NIE potrafi sam dodac kolumny NOT NULL
--  do tabeli z istniejacymi wierszami, dlatego zmieniamy nazwe recznie.
--  Bezpieczne i idempotentne — uruchom raz na bazie Postgres (schemat monopoly).
-- =====================================================================

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'monopoly' AND table_name = 'users'
                 AND column_name = 'balance')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'monopoly' AND table_name = 'users'
                 AND column_name = 'coins') THEN

        ALTER TABLE monopoly.users RENAME COLUMN balance TO coins;
        ALTER TABLE monopoly.users ALTER COLUMN coins DROP DEFAULT;
        ALTER TABLE monopoly.users ALTER COLUMN coins TYPE integer USING round(coins)::integer;
        ALTER TABLE monopoly.users ALTER COLUMN coins SET DEFAULT 1000;
        ALTER TABLE monopoly.users ALTER COLUMN coins SET NOT NULL;

        RAISE NOTICE 'Zmigrowano balance -> coins.';
    ELSE
        RAISE NOTICE 'Migracja pominieta (coins juz istnieje lub brak balance).';
    END IF;
END $$;

-- Kolumne weryfikacji legitymacji Hibernate dodaje sam (nullable):
ALTER TABLE monopoly.users ADD COLUMN IF NOT EXISTS verification_doc_url varchar(255);
