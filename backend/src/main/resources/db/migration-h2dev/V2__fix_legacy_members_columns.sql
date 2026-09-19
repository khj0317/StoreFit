-- Lives in db/migration-h2dev (not the shared db/migration), so it only ever runs against
-- local H2 dev databases -- prod's MySQL always starts from V1's already-correct schema, and
-- this uses H2-specific syntax (ALTER COLUMN ... SET NOT NULL, ADD CONSTRAINT IF NOT EXISTS)
-- that real MySQL doesn't accept.
--
-- Retrofits `members` on a DB that predates V1 -- one whose `members` table already existed
-- (built up by the old ddl-auto: update) before `username` became required and before `email`
-- became optional. ddl-auto could add new columns/constraints but never had a way to backfill
-- a NOT NULL value for existing rows, nor to relax a NOT NULL it had already applied, so those
-- two changes silently failed on any DB with pre-existing member rows. No-op on a DB that got
-- its `members` table fresh from V1 above (it already has both columns right).
ALTER TABLE members ADD COLUMN IF NOT EXISTS username VARCHAR(255);
UPDATE members SET username = CONCAT('user', id) WHERE username IS NULL;
ALTER TABLE members ALTER COLUMN username SET NOT NULL;
ALTER TABLE members ADD CONSTRAINT IF NOT EXISTS uk_members_username UNIQUE (username);

ALTER TABLE members ALTER COLUMN email SET NULL;
