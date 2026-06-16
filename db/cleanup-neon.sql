-- =====================================================================
-- Czyszczenie schematu monopoly na Neon
-- Uruchom w Neon SQL Editor (lub psql) — NAJPIERW zrób backup / branch.
--
-- ZOSTAWIA m.in.: player_achievements (aktywne osiągnięcia!)
-- USUWA: starą tabelę achievements + inne martwe tabele + sierocze kolumny
-- =====================================================================

SET search_path TO monopoly, public;

-- ---------------------------------------------------------------------
-- 1. Martwe tabele (stary kod — NIE mylić z player_achievements)
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS achievements   CASCADE;  -- stara tabela; nowa to player_achievements
DROP TABLE IF EXISTS game_logs       CASCADE;
DROP TABLE IF EXISTS properties      CASCADE;
DROP TABLE IF EXISTS monopoly_cards  CASCADE;
DROP TABLE IF EXISTS board_tiles     CASCADE;
DROP TABLE IF EXISTS board_cards     CASCADE;
DROP TABLE IF EXISTS ranks           CASCADE;
DROP TABLE IF EXISTS daily_tasks     CASCADE;

-- ---------------------------------------------------------------------
-- 2. Sierocze kolumny (brak w JPA / nigdy nieużywane)
-- ---------------------------------------------------------------------
ALTER TABLE game_sessions DROP COLUMN IF EXISTS state_epoch;
ALTER TABLE player_statistics DROP COLUMN IF EXISTS last_login;

-- balance: jeśli masz już coins, usuń starą kolumnę balance (jeśli została obok coins)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'monopoly' AND table_name = 'users' AND column_name = 'coins'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'monopoly' AND table_name = 'users' AND column_name = 'balance'
    ) THEN
        ALTER TABLE monopoly.users DROP COLUMN balance;
        RAISE NOTICE 'Usunieto zduplikowana kolumne users.balance (jest juz coins).';
    ELSIF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'monopoly' AND table_name = 'users' AND column_name = 'balance'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'monopoly' AND table_name = 'users' AND column_name = 'coins'
    ) THEN
        RAISE NOTICE 'Masz tylko balance — uruchom najpierw db/migrate-balance-to-coins.sql';
    END IF;
END $$;

-- ---------------------------------------------------------------------
-- 3. Kontrola po czyszczeniu
-- ---------------------------------------------------------------------
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'monopoly'
ORDER BY table_name;
