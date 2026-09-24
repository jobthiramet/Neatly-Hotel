-- Snapshot of the full Supabase schema (tables, constraints, indexes) after 001–014.
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
  total_units integer not null default 4,
  bed_type varchar(30) not null default 'DOUBLE',
  size_sqm integer not null default 1,
  description text not null default '',
  deleted_at timestamptz,
  constraint room_types_bed_type_check check (bed_type in ('SINGLE', 'DOUBLE', 'KING', 'TWIN')),
  constraint room_types_values_check check (price_per_night > 0 and size_sqm > 0 and capacity between 2 and 6
    and (promotion_price is null or (promotion_price > 0 and promotion_price < price_per_night))),
  constraint room_types_total_units_check check (total_units >= 1)
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

create table if not exists promotion_codes (
  id uuid primary key,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  code varchar(40) not null,
  discount_type varchar(10) not null default 'FIXED',
  amount_off numeric(12, 2),
  percent_off numeric(5, 2),
  min_purchase_amount numeric(12, 2) not null default 0,
  active boolean not null default true,
  deleted_at timestamptz,
  constraint promotion_codes_discount_check check (
    (
      discount_type = 'FIXED'
      and amount_off is not null
      and amount_off > 0
      and percent_off is null
    )
    or (
      discount_type = 'PERCENT'
      and percent_off is not null
      and percent_off > 0
      and percent_off <= 100
      and amount_off is null
    )
  ),
  constraint promotion_codes_min_purchase_check check (min_purchase_amount >= 0)
);
create unique index if not exists promotion_codes_code_key
  on promotion_codes (upper(code))
  where deleted_at is null;

create table if not exists promotion_code_room_types (
  promotion_code_id uuid not null references promotion_codes (id) on delete cascade,
  room_type_id uuid not null references room_types (id) on delete restrict,
  primary key (promotion_code_id, room_type_id)
);

create table if not exists bookings (
  id uuid primary key default gen_random_uuid(),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  booking_number varchar(20) not null unique,
  user_id varchar(64) not null references user_profiles (clerk_user_id) on delete restrict,
  check_in date not null,
  check_out date not null,
  guests integer not null,
  status varchar(30) not null default 'PENDING_PAYMENT',
  special_request text,
  total_price numeric(12, 2) not null,
  cancelled_at timestamptz,
  promotion_code_id uuid references promotion_codes (id),
  payment_method varchar(20) not null,
  guest_first_name varchar(100) not null,
  guest_last_name varchar(100) not null,
  guest_email varchar(254) not null,
  guest_phone varchar(32) not null,
  guest_country varchar(100) not null,
  guest_date_of_birth date not null,
  standard_requests text not null default '[]',
  additional_request text,
  currency varchar(3) not null default 'THB',
  room_subtotal numeric(12, 2) not null,
  extras_total numeric(12, 2) not null,
  discount_total numeric(12, 2) not null,
  room_name_snapshot varchar(120) not null,
  room_image_url text,
  hold_expires_at timestamptz,
  checked_in_at timestamptz,
  constraint bookings_dates_check check (check_out > check_in),
  constraint bookings_guests_check check (guests between 1 and 6),
  constraint bookings_total_price_check check (total_price >= 0),
  constraint bookings_status_check check (status in (
    'PENDING_PAYMENT', 'CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'COMPLETED', 'CANCELLED', 'EXPIRED'
  )),
  constraint bookings_payment_method_check check (payment_method in ('STRIPE', 'CASH'))
);
create index if not exists bookings_user_id_idx on bookings (user_id);
create index if not exists bookings_check_in_idx on bookings (check_in);
create index if not exists bookings_user_created_idx on bookings (user_id, created_at desc);
create index if not exists bookings_status_idx on bookings (status);

-- stay and cancelled are kept in sync with bookings by triggers (010_bookings.sql; EXPIRED also
-- cancels the stay in 011_bookings_checkout.sql).
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

create table if not exists booking_items (
  id uuid primary key,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  booking_id uuid not null references bookings (id) on delete cascade,
  kind varchar(20) not null,
  code varchar(80) not null,
  label varchar(200) not null,
  quantity integer not null default 1,
  unit_price numeric(12, 2) not null,
  amount numeric(12, 2) not null,
  sort_order integer not null default 0,
  constraint booking_items_kind_check check (kind in ('ROOM', 'ADDON', 'DISCOUNT')),
  constraint booking_items_quantity_check check (quantity >= 1)
);
create index if not exists booking_items_booking_idx on booking_items (booking_id, sort_order);

create table if not exists payments (
  id uuid primary key,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  booking_id uuid not null references bookings (id) on delete cascade,
  parent_payment_id uuid references payments (id),
  provider varchar(20) not null,
  kind varchar(20) not null,
  status varchar(20) not null,
  amount numeric(12, 2) not null,
  currency varchar(3) not null default 'THB',
  stripe_checkout_session_id varchar(255),
  stripe_payment_intent_id varchar(255),
  stripe_refund_id varchar(255),
  card_brand varchar(40),
  card_last4 varchar(4),
  paid_at timestamptz,
  failure_message text,
  constraint payments_provider_check check (provider in ('STRIPE', 'CASH')),
  constraint payments_kind_check check (kind in ('CHARGE', 'REFUND')),
  constraint payments_status_check check (status in (
    'PENDING', 'UNPAID', 'SUCCEEDED', 'FAILED', 'CANCELLED'
  )),
  constraint payments_amount_check check (amount > 0)
);
create index if not exists payments_booking_idx on payments (booking_id);
create unique index if not exists payments_stripe_session_key
  on payments (stripe_checkout_session_id) where stripe_checkout_session_id is not null;
create unique index if not exists payments_stripe_intent_key
  on payments (stripe_payment_intent_id) where stripe_payment_intent_id is not null;
create unique index if not exists payments_stripe_refund_key
  on payments (stripe_refund_id) where stripe_refund_id is not null;

create table if not exists stripe_events (
  event_id varchar(255) primary key,
  type varchar(120) not null,
  stripe_object_id varchar(255),
  processed_at timestamptz not null
);

-- Guest chatbot script. One row. topics is a JSON array stored as text.
create table if not exists chatbot_script (
  id uuid primary key,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  greeting text not null,
  auto_reply text not null,
  topics text not null
);
create unique index if not exists chatbot_script_singleton on chatbot_script ((true));
