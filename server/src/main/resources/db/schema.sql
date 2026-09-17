-- Paste into Supabase Dashboard → SQL Editor, then Run.
-- Required before starting the backend with profile=supabase (ddl-auto=validate).

create table if not exists room_types (
  id uuid primary key,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  name varchar(120) not null,
  type varchar(50) not null,
  price_per_night numeric(12, 2) not null,
  capacity integer not null default 2,
  active boolean not null default true
);
