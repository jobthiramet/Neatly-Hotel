-- Supabase only (Postgres). Run in Supabase SQL Editor after 007_rename_rooms_to_room_types.sql,
-- before seeding room types with seed-rooms.ps1. Safe to run more than once.
-- Local (H2) gets the same shape from the entities and local_rooms_seed.sql.

-- 1. 005_rooms_active.sql re-added columns that 005_rooms.sql dropped. Remove them whichever ran last.
alter table room_types drop column if exists type;
alter table room_types drop column if exists active;

-- 2. room_images -> room_type_images, room_id -> room_type_id.
do $$
begin
  if to_regclass('public.room_images') is not null and to_regclass('public.room_type_images') is null then
    alter table public.room_images rename to room_type_images;
  end if;
  if exists (
    select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'room_type_images' and column_name = 'room_id'
  ) then
    alter table public.room_type_images rename column room_id to room_type_id;
  end if;
  if exists (select 1 from pg_constraint where conname = 'room_images_pkey') then
    alter table public.room_type_images rename constraint room_images_pkey to room_type_images_pkey;
  end if;
  if exists (select 1 from pg_constraint where conname = 'room_images_room_id_fkey') then
    alter table public.room_type_images rename constraint room_images_room_id_fkey to room_type_images_room_type_id_fkey;
  end if;
end $$;

alter index if exists room_images_main_key rename to room_type_images_main_key;
alter index if exists room_images_room_sort_idx rename to room_type_images_room_type_sort_idx;

-- 3. Amenities table, filled from the room_types.amenities array (first spelling wins, order kept).
create table if not exists amenities (
  id uuid primary key,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  name varchar(120) not null
);
create unique index if not exists amenities_name_key on amenities (lower(name));

-- Primary key matches the JPA @OrderColumn list. No unique (room_type_id, amenity_id): reordering
-- updates rows in place and would trip it; the service removes duplicates instead.
create table if not exists room_type_amenities (
  room_type_id uuid not null references room_types (id) on delete cascade,
  amenity_id uuid not null references amenities (id) on delete restrict,
  sort_order integer not null,
  primary key (room_type_id, sort_order)
);
create index if not exists room_type_amenities_amenity_id_idx on room_type_amenities (amenity_id);

do $$
begin
  if exists (
    select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'room_types' and column_name = 'amenities'
  ) then
    insert into amenities (id, created_at, updated_at, name)
    select gen_random_uuid(), now(), now(), min(trim(a.name))
    from room_types t cross join lateral unnest(t.amenities) as a (name)
    where trim(a.name) <> ''
    group by lower(trim(a.name))
    on conflict do nothing;

    insert into room_type_amenities (room_type_id, amenity_id, sort_order)
    select room_type_id, amenity_id, row_number() over (partition by room_type_id order by first_ord) - 1
    from (
      select t.id as room_type_id, am.id as amenity_id, min(a.ord) as first_ord
      from room_types t
      cross join lateral unnest(t.amenities) with ordinality as a (name, ord)
      join amenities am on lower(am.name) = lower(trim(a.name))
      group by t.id, am.id
    ) per_room
    on conflict do nothing;

    alter table room_types drop column amenities;
  end if;
end $$;

-- 4. Upgrade the first version of the room units file (then numbered 008) if it already ran:
--    housekeeping statuses replace VACANT, units get floor, deleted_at and the room number check.
do $$
begin
  if to_regclass('public.room_units') is null then
    return;
  end if;
  update room_statuses set code = 'CLEAN', label = 'Clean', sort_order = 3, updated_at = now() where code = 'VACANT';
  alter table room_units add column if not exists floor smallint;
  update room_units set floor = (cast(room_number as integer) - 1) / 10 + 1 where floor is null;
  alter table room_units alter column floor set not null;
  alter table room_units add column if not exists deleted_at timestamptz;
  if not exists (select 1 from pg_constraint where conname = 'room_units_room_number_format') then
    alter table room_units add constraint room_units_room_number_format
      check (regexp_like(room_number, '^[0-9]{4}$'));
  end if;
  if not exists (select 1 from pg_constraint where conname = 'room_units_floor_check') then
    alter table room_units add constraint room_units_floor_check check (floor > 0);
  end if;
end $$;

-- New tables are backend-only (the API connects as the database owner), like hotel_info and user_profiles.
alter table amenities enable row level security;
alter table room_type_amenities enable row level security;
