-- Local (H2) profile only. Same promo as 008_bookings_and_payments.sql.
insert into promotion_codes (id, created_at, updated_at, code, amount_off, active)
select '00000000-0000-0000-0002-000000000001', timestamp '2026-09-17 00:00:00', timestamp '2026-09-17 00:00:00', 'NEATLYNEW400', 400.00, true
where not exists (select 1 from promotion_codes);
