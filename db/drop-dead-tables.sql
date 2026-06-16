-- Usuniecie nieuzywanych tabel (martwy kod usuniety z aplikacji).
-- Encje Property, MonopolyCard, BoardTile, BoardCard, Rank, DailyTask,
-- Achievement, GameLog nie sa juz mapowane ani czytane przez aplikacje.
-- ddl-auto=update nigdy nie usuwa tabel, wiec trzeba zrobic to recznie.
--
-- Uruchom na bazie Neon (schemat monopoly), np.:
--   psql "postgresql://neondb_owner:***@ep-damp-fire-alzendby-pooler.c-3.eu-central-1.aws.neon.tech/neondb?sslmode=require" -f db/drop-dead-tables.sql
-- lub w kliencie SQL po: SET search_path TO monopoly;

SET search_path TO monopoly;

DROP TABLE IF EXISTS achievements   CASCADE;
DROP TABLE IF EXISTS game_logs       CASCADE;
DROP TABLE IF EXISTS properties      CASCADE;
DROP TABLE IF EXISTS monopoly_cards  CASCADE;
DROP TABLE IF EXISTS board_tiles     CASCADE;
DROP TABLE IF EXISTS board_cards     CASCADE;
DROP TABLE IF EXISTS ranks           CASCADE;
DROP TABLE IF EXISTS daily_tasks     CASCADE;
