-- =====================================================================
--  Tworzenie bazy danych PostgreSQL dla projektu "Gra o Przetrwanie".
--
--  UWAGA: Tabele tworzy automatycznie Hibernate (spring.jpa.hibernate.ddl-auto
--  = update w profilu postgres). Ten skrypt przygotowuje SAMA BAZE i UZYTKOWNIKA.
--
--  Jak uruchomic (gdy PostgreSQL bedzie zainstalowany), w konsoli psql jako
--  superuser (np. "postgres"):
--      psql -U postgres -f db/postgres-setup.sql
-- =====================================================================

-- 1. Uzytkownik (rola) bazodanowy aplikacji
CREATE USER monopoly WITH PASSWORD 'monopoly';

-- 2. Baza danych nalezaca do tego uzytkownika
CREATE DATABASE monopoly OWNER monopoly ENCODING 'UTF8';

-- 3. Uprawnienia
GRANT ALL PRIVILEGES ON DATABASE monopoly TO monopoly;

-- Po wykonaniu uruchom aplikacje z profilem postgres:
--     mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
-- Hibernate sam utworzy tabele: users, player_statistics, properties,
-- monopoly_cards, game_logs, achievements.
