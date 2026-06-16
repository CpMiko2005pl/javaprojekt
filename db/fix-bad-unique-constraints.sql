-- Usuwa BLEDNE pojedynczkolumnowe UNIQUE na FK (Hibernate czasem je tworzy).
-- Zostawia tylko poprawne UNIQUE zlozone zgodne z JPA.
-- Uruchom w Neon SQL Editor: SET search_path TO monopoly; potem caly plik.

SET search_path TO monopoly, public;

-- 1) Podglad — co jest teraz (opcjonalnie)
SELECT
    c.conrelid::regclass AS tabela,
    c.conname            AS constraint_name,
    pg_get_constraintdef(c.oid) AS definicja
FROM pg_constraint c
JOIN pg_class t ON t.oid = c.conrelid
JOIN pg_namespace n ON n.oid = t.relnamespace
WHERE n.nspname = 'monopoly'
  AND c.contype = 'u'
  AND t.relname IN (
      'friendships', 'player_achievements',
      'profile_comments', 'profile_comment_likes'
  )
ORDER BY t.relname, c.conname;

-- 2) Usun pojedynczkolumnowe UNIQUE (zostawia zlozone pary/trójki)
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT c.conname AS cname, n.nspname AS schemaname, t.relname AS tablename
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE n.nspname = 'monopoly'
          AND c.contype = 'u'
          AND t.relname IN (
              'friendships', 'player_achievements',
              'profile_comments', 'profile_comment_likes'
          )
          AND array_length(c.conkey, 1) = 1
    LOOP
        EXECUTE format(
            'ALTER TABLE %I.%I DROP CONSTRAINT %I',
            r.schemaname, r.tablename, r.cname
        );
        RAISE NOTICE 'Usunieto: %.% -> %', r.schemaname, r.tablename, r.cname;
    END LOOP;
END $$;

-- 3) Kontrola — powinny zostac tylko zlozone UNIQUE:
SELECT
    c.conrelid::regclass AS tabela,
    c.conname,
    pg_get_constraintdef(c.oid) AS definicja
FROM pg_constraint c
JOIN pg_class t ON t.oid = c.conrelid
JOIN pg_namespace n ON n.oid = t.relnamespace
WHERE n.nspname = 'monopoly'
  AND c.contype = 'u'
  AND t.relname IN (
      'friendships', 'player_achievements',
      'profile_comments', 'profile_comment_likes'
  )
ORDER BY t.relname, c.conname;
