-- Optimistic locking: every update checks and increments the row's version, so of two concurrent
-- updates of the same row the second fails (409) instead of silently overwriting the first.

ALTER TABLE city ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE airport ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE airline ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE airplane ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE flight ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
