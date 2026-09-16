-- Run in Supabase SQL Editor after 004_user_profiles.sql (and schema.sql, which creates rooms).
-- Admin Room & Property: room fields from Figma, soft delete, images and amenities.
-- Drops rooms.type and rooms.active: Figma has no such fields; deleted_at replaces active.

alter table rooms drop column if exists type;
alter table rooms drop column if exists active;

alter table rooms add column if not exists bed_type varchar(30) not null default 'DOUBLE';
alter table rooms add column if not exists size_sqm integer not null default 1;
alter table rooms add column if not exists promotion_price numeric(12, 2);
alter table rooms add column if not exists description text not null default '';
alter table rooms add column if not exists amenities text[] not null default '{}';
alter table rooms add column if not exists deleted_at timestamptz;

alter table rooms drop constraint if exists rooms_bed_type_check;
alter table rooms add constraint rooms_bed_type_check
  check (bed_type in ('SINGLE', 'DOUBLE', 'KING', 'TWIN'));
alter table rooms drop constraint if exists rooms_values_check;
alter table rooms add constraint rooms_values_check
  check (price_per_night > 0 and size_sqm > 0 and capacity between 2 and 6
    and (promotion_price is null or (promotion_price > 0 and promotion_price < price_per_night)));

-- A deleted room's name can be reused.
create unique index if not exists rooms_name_active_key on rooms (lower(name)) where deleted_at is null;
create index if not exists rooms_active_created_at_idx on rooms (created_at desc) where deleted_at is null;

create table if not exists room_images (
  id uuid primary key,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  room_id uuid not null references rooms (id),
  url text not null,
  storage_path text not null,
  sort_order integer not null default 0,
  is_main boolean not null default false
);

-- One main image per room.
create unique index if not exists room_images_main_key on room_images (room_id) where is_main;
create index if not exists room_images_room_sort_idx on room_images (room_id, sort_order);
