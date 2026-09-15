-- Admin Room Management fields for `rooms`.
-- Run in Supabase SQL Editor if an older `rooms` table (name/type only) already exists.
-- Fresh projects can rely on schema.sql instead.

alter table rooms add column if not exists room_number varchar(20);
alter table rooms add column if not exists room_type varchar(120);
alter table rooms add column if not exists bed_type varchar(50);
alter table rooms add column if not exists status varchar(50);
alter table rooms add column if not exists price_per_night numeric(12, 2);
alter table rooms add column if not exists capacity integer;
alter table rooms add column if not exists active boolean;

-- Backfill from legacy columns when present.
do $$
begin
  if exists (
    select 1 from information_schema.columns
    where table_name = 'rooms' and column_name = 'name'
  ) then
    update rooms
    set room_type = coalesce(nullif(room_type, ''), name)
    where room_type is null or room_type = '';
  end if;
end $$;

update rooms set room_number = lpad(substr(replace(id::text, '-', ''), 1, 4), 4, '0')
where room_number is null or room_number = '';
update rooms set room_type = coalesce(nullif(room_type, ''), 'Superior')
where room_type is null or room_type = '';
update rooms set bed_type = coalesce(nullif(bed_type, ''), 'Single Bed')
where bed_type is null or bed_type = '';
update rooms set status = coalesce(nullif(status, ''), 'Vacant Clean')
where status is null or status = '';
update rooms set price_per_night = coalesce(price_per_night, 0) where price_per_night is null;
update rooms set capacity = coalesce(capacity, 2) where capacity is null;
update rooms set active = coalesce(active, true) where active is null;

alter table rooms alter column room_number set not null;
alter table rooms alter column room_type set not null;
alter table rooms alter column bed_type set not null;
alter table rooms alter column status set not null;
alter table rooms alter column price_per_night set not null;
alter table rooms alter column capacity set not null;
alter table rooms alter column active set not null;

do $$
begin
  if not exists (
    select 1 from pg_constraint where conname = 'uk_rooms_room_number'
  ) then
    alter table rooms add constraint uk_rooms_room_number unique (room_number);
  end if;
end $$;

alter table rooms drop column if exists name;
alter table rooms drop column if exists type;
