-- Run in Supabase SQL Editor after 011_bookings_checkout.sql. Safe to run more than once.
-- feat/payment's old 008 bookings table kept room_type_id / rooms_count / grand_total.
-- Checkout now stores rooms on booking_rooms and the total on total_price; Hibernate
-- does not write those leftover columns, so inserts fail with 23502.

do $$
begin
  if exists (
    select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'bookings' and column_name = 'grand_total'
  ) then
    update public.bookings
    set total_price = grand_total
    where total_price is null;
  end if;
end $$;

alter table bookings drop column if exists room_type_id;
alter table bookings drop column if exists rooms_count;
alter table bookings drop column if exists grand_total;

drop index if exists bookings_room_dates_idx;
