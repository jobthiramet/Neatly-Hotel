-- Paste into Supabase Dashboard → SQL Editor, then Run.
-- Required before starting the backend with profile=supabase (ddl-auto=validate).
-- Prefer numbered scripts under this folder for incremental changes; keep this file as the full baseline.

create table if not exists rooms (
  id uuid primary key,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  room_number varchar(20) not null,
  room_type varchar(120) not null,
  bed_type varchar(50) not null,
  status varchar(50) not null default 'Vacant Clean',
  price_per_night numeric(12, 2) not null default 0,
  capacity integer not null default 2,
  active boolean not null default true,
  constraint uk_rooms_room_number unique (room_number)
);
