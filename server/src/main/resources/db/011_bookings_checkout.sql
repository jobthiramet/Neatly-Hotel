-- Run in Supabase SQL Editor after 010_bookings.sql. Safe to run more than once.
-- Guest checkout: Stripe/cash payments, promo codes, line items, and booking_rooms rows
-- created at checkout (room_unit_id stays null until a physical room is assigned).

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

-- 010 created a slim bookings table; 008 on feat/payment used clerk_user_id. Fold both shapes here.
do $$
begin
  if exists (
    select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'bookings' and column_name = 'clerk_user_id'
  ) and not exists (
    select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'bookings' and column_name = 'user_id'
  ) then
    alter table public.bookings rename column clerk_user_id to user_id;
  end if;
  if exists (
    select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'bookings' and column_name = 'clerk_user_id'
  ) and exists (
    select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'bookings' and column_name = 'user_id'
  ) then
    update public.bookings set user_id = clerk_user_id where user_id is null;
    alter table public.bookings drop column clerk_user_id;
  end if;
end $$;

alter table bookings add column if not exists booking_number varchar(20);
update bookings
set booking_number = 'N' || upper(substr(replace(id::text, '-', ''), 1, 16))
where booking_number is null;
alter table bookings alter column booking_number set not null;
create unique index if not exists bookings_booking_number_key on bookings (booking_number);

alter table bookings add column if not exists user_id varchar(64);
do $$
begin
  if exists (
    select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'bookings' and column_name = 'user_id'
  ) and not exists (
    select 1 from pg_constraint where conname = 'bookings_user_id_fkey'
  ) then
    alter table public.bookings
      add constraint bookings_user_id_fkey
      foreign key (user_id) references user_profiles (clerk_user_id) on delete restrict;
  end if;
end $$;

alter table bookings alter column status type varchar(30);
update bookings set status = 'PENDING_PAYMENT' where status = 'PENDING';
alter table bookings alter column status set default 'PENDING_PAYMENT';
alter table bookings drop constraint if exists bookings_status_check;
alter table bookings add constraint bookings_status_check check (status in (
  'PENDING_PAYMENT', 'CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'COMPLETED', 'CANCELLED', 'EXPIRED'
));

alter table bookings drop constraint if exists bookings_guests_check;
alter table bookings add constraint bookings_guests_check check (guests between 1 and 6);

alter table bookings add column if not exists promotion_code_id uuid references promotion_codes (id);
alter table bookings add column if not exists payment_method varchar(20) not null default 'CASH';
alter table bookings drop constraint if exists bookings_payment_method_check;
alter table bookings add constraint bookings_payment_method_check check (payment_method in ('STRIPE', 'CASH'));

alter table bookings add column if not exists guest_first_name varchar(100) not null default '';
alter table bookings add column if not exists guest_last_name varchar(100) not null default '';
alter table bookings add column if not exists guest_email varchar(254) not null default '';
alter table bookings add column if not exists guest_phone varchar(32) not null default '';
alter table bookings add column if not exists guest_country varchar(100) not null default '';
alter table bookings add column if not exists guest_date_of_birth date not null default date '1970-01-01';
alter table bookings add column if not exists standard_requests text not null default '[]';
alter table bookings add column if not exists additional_request text;
alter table bookings add column if not exists currency varchar(3) not null default 'THB';
alter table bookings add column if not exists total_price numeric(12, 2);
alter table bookings add column if not exists room_subtotal numeric(12, 2) not null default 0;
alter table bookings add column if not exists extras_total numeric(12, 2) not null default 0;
alter table bookings add column if not exists discount_total numeric(12, 2) not null default 0;
alter table bookings add column if not exists room_name_snapshot varchar(120) not null default '';
alter table bookings add column if not exists room_image_url text;
alter table bookings add column if not exists hold_expires_at timestamptz;
alter table bookings add column if not exists checked_in_at timestamptz;

do $$
begin
  if exists (
    select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'bookings' and column_name = 'grand_total'
  ) then
    update public.bookings set total_price = grand_total where total_price is null;
  end if;
end $$;
update bookings set total_price = 0 where total_price is null;
alter table bookings alter column total_price set not null;

create index if not exists bookings_user_created_idx on bookings (user_id, created_at desc);
create index if not exists bookings_status_idx on bookings (status);

create or replace function booking_rooms_copy_booking() returns trigger
language plpgsql as $$
begin
  select daterange(b.check_in, b.check_out), b.status in ('CANCELLED', 'EXPIRED')
  into new.stay, new.cancelled
  from bookings b where b.id = new.booking_id;
  return new;
end $$;

create or replace function bookings_sync_rooms() returns trigger
language plpgsql as $$
begin
  update booking_rooms
  set stay = daterange(new.check_in, new.check_out),
      cancelled = new.status in ('CANCELLED', 'EXPIRED'),
      updated_at = now()
  where booking_id = new.id;
  return new;
end $$;

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
select '00000000-0000-0000-0005-000000000001', now(), now(), 'NEATLYNEW400', 400.00, true
where not exists (
  select 1 from promotion_codes where upper(code) = 'NEATLYNEW400'
);

alter table promotion_codes enable row level security;
alter table booking_items enable row level security;
alter table payments enable row level security;
alter table stripe_events enable row level security;
