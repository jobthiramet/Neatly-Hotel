-- Snapshot of the full Supabase schema (tables, constraints, indexes) after 001–010.
-- Reference for reading the model. For setup, run the numbered files in order; they also create storage buckets,
-- row level security, policies, triggers, views and seed data. Every statement below is safe to re-run.
-- Keep this file in sync when a numbered migration changes a table.

create extension if not exists btree_gist;

create table if not exists hotel_info (
  id uuid primary key,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  name varchar(120) not null,
  description text not null,
  logo_url varchar(500)
);
create unique index if not exists hotel_info_singleton on hotel_info ((true));

create table if not exists user_profiles (
  clerk_user_id varchar(64) primary key,
  first_name varchar(100) not null,
  last_name varchar(100) not null,
  phone_number varchar(16) not null,
  date_of_birth date not null,
  country varchar(100) not null,
  profile_picture text,
  role varchar(20) not null default 'user',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint user_profiles_phone_number_format check (phone_number ~ '^\+[1-9][0-9]{7,14}$'),
  constraint user_profiles_role_check check (role in ('user', 'agent'))
);

-- What the hotel sells. Soft deleted.
create table if not exists room_types (
  id uuid primary key,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  name varchar(120) not null,
  price_per_night numeric(12, 2) not null,
  promotion_price numeric(12, 2),
  capacity integer not null default 2,
  bed_type varchar(30) not null default 'DOUBLE',
  size_sqm integer not null default 1,
  description text not null default '',
  deleted_at timestamptz,
  constraint room_types_bed_type_check check (bed_type in ('SINGLE', 'DOUBLE', 'KING', 'TWIN')),
  constraint room_types_values_check check (price_per_night > 0 and size_sqm > 0 and capacity between 2 and 6
    and (promotion_price is null or (promotion_price > 0 and promotion_price < price_per_night)))
);
create unique index if not exists room_types_name_active_key on room_types (lower(name)) where deleted_at is null;
create index if not exists room_types_active_created_at_idx on room_types (created_at desc) where deleted_at is null;

create table if not exists room_type_images (
  id uuid primary key,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  room_type_id uuid not null references room_types (id),
  url text not null,
  storage_path text not null,
  sort_order integer not null default 0,
  is_main boolean not null default false
);
create unique index if not exists room_type_images_main_key on room_type_images (room_type_id) where is_main;
create index if not exists room_type_images_room_type_sort_idx on room_type_images (room_type_id, sort_order);

create table if not exists amenities (
  id uuid primary key,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  name varchar(120) not null
);
create unique index if not exists amenities_name_key on amenities (lower(name));

create table if not exists room_type_amenities (
  room_type_id uuid not null references room_types (id) on delete cascade,
  amenity_id uuid not null references amenities (id) on delete restrict,
  sort_order integer not null,
  primary key (room_type_id, sort_order)
);
create index if not exists room_type_amenities_amenity_id_idx on room_type_amenities (amenity_id);

-- Housekeeping status. Occupancy is derived from bookings (view room_unit_occupancy).
create table if not exists room_statuses (
  id uuid primary key,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  code varchar(50) not null unique,
  label varchar(100) not null,
  sort_order integer not null
);

-- Physical rooms.
create table if not exists room_units (
  id uuid primary key,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  room_number varchar(10) not null unique,
  floor smallint not null,
  room_type_id uuid not null references room_types (id) on delete restrict,
  room_status_id uuid not null references room_statuses (id) on delete restrict,
  deleted_at timestamptz,
  constraint room_units_room_number_format check (regexp_like(room_number, '^[0-9]{4}$')),
  constraint room_units_floor_check check (floor > 0)
);
create index if not exists room_units_room_type_id_idx on room_units (room_type_id);
create index if not exists room_units_room_status_id_idx on room_units (room_status_id);

create table if not exists bookings (
  id uuid primary key default gen_random_uuid(),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  booking_number varchar(20) not null unique,
  user_id varchar(64) not null references user_profiles (clerk_user_id) on delete restrict,
  check_in date not null,
  check_out date not null,
  guests integer not null,
  status varchar(20) not null default 'PENDING',
  special_request text,
  total_price numeric(12, 2) not null,
  cancelled_at timestamptz,
  constraint bookings_dates_check check (check_out > check_in),
  constraint bookings_guests_check check (guests > 0),
  constraint bookings_total_price_check check (total_price >= 0),
  constraint bookings_status_check check (status in ('PENDING', 'CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED'))
);
create index if not exists bookings_user_id_idx on bookings (user_id);
create index if not exists bookings_check_in_idx on bookings (check_in);

-- stay and cancelled are kept in sync with bookings by triggers (010_bookings.sql).
create table if not exists booking_rooms (
  id uuid primary key default gen_random_uuid(),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  booking_id uuid not null references bookings (id) on delete cascade,
  room_type_id uuid not null references room_types (id) on delete restrict,
  room_unit_id uuid references room_units (id) on delete restrict,
  price_per_night numeric(12, 2) not null,
  stay daterange not null,
  cancelled boolean not null default false,
  constraint booking_rooms_price_check check (price_per_night >= 0),
  constraint booking_rooms_no_double_booking exclude using gist (room_unit_id with =, stay with &&) where (not cancelled)
);
create index if not exists booking_rooms_booking_id_idx on booking_rooms (booking_id);
create index if not exists booking_rooms_room_type_stay_idx on booking_rooms using gist (room_type_id, stay);
