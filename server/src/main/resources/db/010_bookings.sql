-- Supabase only (Postgres: btree_gist, daterange, triggers). Run in Supabase SQL Editor after
-- 009_room_statuses_and_units.sql. Safe to run more than once. No entities or API use these tables yet.

create extension if not exists btree_gist;

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
  -- Snapshot at booking time; later room type price changes do not affect it.
  total_price numeric(12, 2) not null,
  cancelled_at timestamptz,
  constraint bookings_dates_check check (check_out > check_in),
  constraint bookings_guests_check check (guests > 0),
  constraint bookings_total_price_check check (total_price >= 0),
  constraint bookings_status_check check (status in ('PENDING', 'CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED'))
);

create index if not exists bookings_user_id_idx on bookings (user_id);
create index if not exists bookings_check_in_idx on bookings (check_in);

-- One row per booked room. Guests book a room type; room_unit_id is set when a room is assigned.
-- stay and cancelled are copied from the booking by triggers so the exclusion constraint can use them.
create table if not exists booking_rooms (
  id uuid primary key default gen_random_uuid(),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  booking_id uuid not null references bookings (id) on delete cascade,
  room_type_id uuid not null references room_types (id) on delete restrict,
  room_unit_id uuid references room_units (id) on delete restrict,
  -- Snapshot at booking time.
  price_per_night numeric(12, 2) not null,
  stay daterange not null,
  cancelled boolean not null default false,
  constraint booking_rooms_price_check check (price_per_night >= 0)
);

create index if not exists booking_rooms_booking_id_idx on booking_rooms (booking_id);
create index if not exists booking_rooms_room_type_stay_idx on booking_rooms using gist (room_type_id, stay);

-- The same room can never be in two active bookings whose stays overlap ([check_in, check_out)).
do $$
begin
  if not exists (select 1 from pg_constraint where conname = 'booking_rooms_no_double_booking') then
    alter table booking_rooms add constraint booking_rooms_no_double_booking
      exclude using gist (room_unit_id with =, stay with &&) where (not cancelled);
  end if;
end $$;

create or replace function booking_rooms_copy_booking() returns trigger
language plpgsql as $$
begin
  select daterange(b.check_in, b.check_out), b.status = 'CANCELLED'
  into new.stay, new.cancelled
  from bookings b where b.id = new.booking_id;
  return new;
end $$;

drop trigger if exists booking_rooms_copy_booking on booking_rooms;
create trigger booking_rooms_copy_booking
  before insert or update of booking_id, stay, cancelled on booking_rooms
  for each row execute function booking_rooms_copy_booking();

create or replace function bookings_sync_rooms() returns trigger
language plpgsql as $$
begin
  update booking_rooms
  set stay = daterange(new.check_in, new.check_out), cancelled = new.status = 'CANCELLED', updated_at = now()
  where booking_id = new.id;
  return new;
end $$;

drop trigger if exists bookings_sync_rooms on bookings;
create trigger bookings_sync_rooms
  after update of check_in, check_out, status on bookings
  for each row execute function bookings_sync_rooms();

-- Occupancy is derived, not stored: a unit is occupied while a checked-in booking covers today.
-- The Room Management badge combines it with the housekeeping status, e.g. "Occupied" + "Dirty".
create or replace view room_unit_occupancy with (security_invoker = true) as
select
  u.id as room_unit_id,
  u.room_number,
  exists (
    select 1
    from booking_rooms br
    join bookings b on b.id = br.booking_id
    where br.room_unit_id = u.id and b.status = 'CHECKED_IN' and br.stay @> current_date
  ) as occupied
from room_units u
where u.deleted_at is null;

-- Backend-only tables (the API connects as the database owner), like hotel_info and user_profiles.
alter table room_statuses enable row level security;
alter table room_units enable row level security;
alter table bookings enable row level security;
alter table booking_rooms enable row level security;
