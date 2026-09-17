-- Run in Supabase SQL Editor after 007_rename_rooms_to_room_types.sql.
-- Guest checkout: bookings, priced line items, Stripe/cash payments, promo codes.
-- Safe to run more than once.

alter table room_types add column if not exists total_units integer not null default 4;
alter table room_types drop constraint if exists room_types_total_units_check;
alter table room_types add constraint room_types_total_units_check check (total_units >= 1);

create table if not exists promotion_codes (
  id uuid primary key,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  code varchar(40) not null,
  amount_off numeric(12, 2) not null,
  active boolean not null default true,
  constraint promotion_codes_amount_check check (amount_off > 0)
);

create unique index if not exists promotion_codes_code_key on promotion_codes (upper(code));

create table if not exists bookings (
  id uuid primary key,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  clerk_user_id varchar(64) not null,
  room_type_id uuid not null references room_types (id),
  promotion_code_id uuid references promotion_codes (id),
  check_in date not null,
  check_out date not null,
  guests integer not null,
  rooms_count integer not null default 1,
  status varchar(30) not null,
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
  grand_total numeric(12, 2) not null,
  room_name_snapshot varchar(120) not null,
  room_image_url text,
  hold_expires_at timestamptz,
  cancelled_at timestamptz,
  checked_in_at timestamptz,
  constraint bookings_dates_check check (check_out > check_in),
  constraint bookings_guests_check check (guests between 1 and 6),
  constraint bookings_rooms_count_check check (rooms_count >= 1),
  constraint bookings_status_check check (status in (
    'PENDING_PAYMENT', 'CONFIRMED', 'CHECKED_IN', 'COMPLETED', 'CANCELLED', 'EXPIRED'
  )),
  constraint bookings_payment_method_check check (payment_method in ('STRIPE', 'CASH'))
);

create index if not exists bookings_user_created_idx on bookings (clerk_user_id, created_at desc);
create index if not exists bookings_room_dates_idx on bookings (room_type_id, check_in, check_out);
create index if not exists bookings_status_idx on bookings (status);

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

insert into promotion_codes (id, created_at, updated_at, code, amount_off, active)
select '00000000-0000-0000-0002-000000000001', now(), now(), 'NEATLYNEW400', 400.00, true
where not exists (
  select 1 from promotion_codes where upper(code) = 'NEATLYNEW400'
);
