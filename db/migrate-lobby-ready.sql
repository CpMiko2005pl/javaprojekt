-- Migracja: lobby pre-game (gotowosc graczy + lider pokoju)
-- Uruchom w Neon SQL Editor / psql na bazie z schematem monopoly:

ALTER TABLE monopoly.game_players
    ADD COLUMN IF NOT EXISTS ready boolean NOT NULL DEFAULT false;

ALTER TABLE monopoly.game_sessions
    ADD COLUMN IF NOT EXISTS leader_id bigint;
