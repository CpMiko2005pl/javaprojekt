-- Migracja: kolumna state_epoch w game_sessions (wersjonowanie stanu gry / timery bota)
-- Uruchom w pgAdmin lub psql na bazie monopoly:

ALTER TABLE monopoly.game_sessions
    ADD COLUMN IF NOT EXISTS state_epoch integer NOT NULL DEFAULT 0;
