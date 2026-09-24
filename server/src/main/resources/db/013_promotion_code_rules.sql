-- Run in Supabase SQL Editor after 011_bookings_checkout.sql. Safe to run more than once.
-- Promo rules for the admin Promo code page: fixed or percent discount, minimum purchase,
-- optional room-type limit, and soft delete so past bookings can keep the code.

alter table promotion_codes add column if not exists discount_type varchar(10) not null default 'FIXED';
alter table promotion_codes add column if not exists percent_off numeric(5, 2);
alter table promotion_codes add column if not exists min_purchase_amount numeric(12, 2) not null default 0;
alter table promotion_codes add column if not exists deleted_at timestamptz;

alter table promotion_codes drop constraint if exists promotion_codes_amount_check;
alter table promotion_codes alter column amount_off drop not null;

alter table promotion_codes drop constraint if exists promotion_codes_discount_check;
alter table promotion_codes add constraint promotion_codes_discount_check check (
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
);

alter table promotion_codes drop constraint if exists promotion_codes_min_purchase_check;
alter table promotion_codes add constraint promotion_codes_min_purchase_check check (min_purchase_amount >= 0);

drop index if exists promotion_codes_code_key;
create unique index if not exists promotion_codes_code_key
  on promotion_codes (upper(code))
  where deleted_at is null;

create table if not exists promotion_code_room_types (
  promotion_code_id uuid not null references promotion_codes (id) on delete cascade,
  room_type_id uuid not null references room_types (id) on delete restrict,
  primary key (promotion_code_id, room_type_id)
);

alter table promotion_code_room_types enable row level security;
