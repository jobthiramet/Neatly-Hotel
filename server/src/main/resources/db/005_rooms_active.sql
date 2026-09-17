-- Upgrade an existing room_types table; schema.sql only creates missing tables.
-- Existing room types stay active. Safe to run more than once.
-- The legacy table has no room category; mark it unknown until classified.
alter table public.room_types
  add column if not exists active boolean not null default true,
  add column if not exists type varchar(50) not null default 'unknown';
