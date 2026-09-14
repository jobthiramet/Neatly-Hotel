-- Run in Supabase SQL Editor after schema.sql.
-- Single-row table holding the hotel name, description and logo.

create table if not exists hotel_info (
  id uuid primary key,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  name varchar(120) not null,
  description text not null,
  logo_url varchar(500)
);

-- Unique index on a constant: a second row would collide, so only one row can exist.
create unique index if not exists hotel_info_singleton on hotel_info ((true));

-- Block access through the anon/authenticated keys; the server connects as the DB owner.
alter table hotel_info enable row level security;
