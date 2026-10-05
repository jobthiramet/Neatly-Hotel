-- Run in Supabase SQL Editor after 006_storage_room_images.sql.
-- Existing databases created `rooms`; this renames it to `room_types`. Safe to run more than once.

do $$
begin
  if to_regclass('public.rooms') is not null and to_regclass('public.room_types') is null then
    alter table public.rooms rename to room_types;
  end if;
end $$;

alter index if exists rooms_name_active_key rename to room_types_name_active_key;
alter index if exists rooms_active_created_at_idx rename to room_types_active_created_at_idx;

do $$
begin
  if to_regclass('public.room_types') is null then
    return;
  end if;
  if exists (
    select 1 from pg_constraint
    where conname = 'rooms_pkey' and conrelid = 'public.room_types'::regclass
  ) then
    alter table public.room_types rename constraint rooms_pkey to room_types_pkey;
  end if;
  if exists (
    select 1 from pg_constraint
    where conname = 'rooms_bed_type_check' and conrelid = 'public.room_types'::regclass
  ) then
    alter table public.room_types rename constraint rooms_bed_type_check to room_types_bed_type_check;
  end if;
  if exists (
    select 1 from pg_constraint
    where conname = 'rooms_values_check' and conrelid = 'public.room_types'::regclass
  ) then
    alter table public.room_types rename constraint rooms_values_check to room_types_values_check;
  end if;
end $$;
