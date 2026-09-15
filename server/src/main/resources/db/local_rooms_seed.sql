-- Local (H2) profile only: sample rooms so list, search, pagination, edit and delete
-- work without Supabase Storage. Do not run in Supabase.

insert into rooms (id, created_at, updated_at, name, price_per_night, capacity, bed_type, size_sqm, promotion_price, description, amenities)
select * from (values
  ('00000000-0000-0000-0001-000000000001', timestamp '2026-09-01 00:00:00', timestamp '2026-09-01 00:00:00', 'Superior Garden View', 3000.00, 2, 'DOUBLE', 32, 2500.00, 'Rooms (36sqm) with full garden views, 1 single bed, bathroom with bathtub & shower.', array['Safe in Room', 'Air Conditioning']),
  ('00000000-0000-0000-0001-000000000002', timestamp '2026-09-02 00:00:00', timestamp '2026-09-02 00:00:00', 'Deluxe', 3200.00, 2, 'DOUBLE', 32, null, 'Deluxe room.', array['Hairdryer']),
  ('00000000-0000-0000-0001-000000000003', timestamp '2026-09-03 00:00:00', timestamp '2026-09-03 00:00:00', 'Superior', 2800.00, 2, 'SINGLE', 28, null, 'Superior room.', array['Shower']),
  ('00000000-0000-0000-0001-000000000004', timestamp '2026-09-04 00:00:00', timestamp '2026-09-04 00:00:00', 'Premier Sea View', 4500.00, 3, 'KING', 40, 4000.00, 'Premier room with sea view.', array['Minibar']),
  ('00000000-0000-0000-0001-000000000005', timestamp '2026-09-05 00:00:00', timestamp '2026-09-05 00:00:00', 'Supreme', 5000.00, 4, 'TWIN', 45, null, 'Supreme room.', array['Telephone']),
  ('00000000-0000-0000-0001-000000000006', timestamp '2026-09-06 00:00:00', timestamp '2026-09-06 00:00:00', 'Suite', 8000.00, 6, 'KING', 70, 7000.00, 'Suite.', array['Lamp']),
  ('00000000-0000-0000-0001-000000000007', timestamp '2026-09-07 00:00:00', timestamp '2026-09-07 00:00:00', 'Family Twin', 3500.00, 4, 'TWIN', 38, null, 'Family room.', array['Ironing board']),
  ('00000000-0000-0000-0001-000000000008', timestamp '2026-09-08 00:00:00', timestamp '2026-09-08 00:00:00', 'Economy Single', 1500.00, 2, 'SINGLE', 20, null, 'Economy room.', array['Shower']),
  ('00000000-0000-0000-0001-000000000009', timestamp '2026-09-09 00:00:00', timestamp '2026-09-09 00:00:00', 'Honeymoon Suite', 9000.00, 2, 'KING', 60, 8500.00, 'Honeymoon suite.', array['Minibar']),
  ('00000000-0000-0000-0001-000000000010', timestamp '2026-09-10 00:00:00', timestamp '2026-09-10 00:00:00', 'Pool Access', 6000.00, 2, 'DOUBLE', 42, null, 'Pool access room.', array['Safe in Room']),
  ('00000000-0000-0000-0001-000000000011', timestamp '2026-09-11 00:00:00', timestamp '2026-09-11 00:00:00', 'Garden Twin', 2900.00, 2, 'TWIN', 30, null, 'Garden twin room.', array['Lamp']),
  ('00000000-0000-0000-0001-000000000012', timestamp '2026-09-12 00:00:00', timestamp '2026-09-12 00:00:00', 'Executive', 5500.00, 3, 'KING', 50, 5000.00, 'Executive room.', array['Telephone'])
) as seed (id, created_at, updated_at, name, price_per_night, capacity, bed_type, size_sqm, promotion_price, description, amenities)
where not exists (select 1 from rooms);
