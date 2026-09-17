-- Local (H2) profile only: the six Figma room types from seed/rooms.json, without images
-- (storage is not configured locally). Timestamps keep the Figma order in the newest-first list. Supabase rooms are seeded with seed-rooms.ps1. Do not run in Supabase.

insert into room_types (id, created_at, updated_at, name, price_per_night, capacity, bed_type, size_sqm, promotion_price, description)
select * from (values
  ('00000000-0000-0000-0001-000000000001', timestamp '2026-09-16 00:00:00', timestamp '2026-09-16 00:00:00', 'Superior Garden View', 3000.00, 2, 'DOUBLE', 32, 2500.00, 'Rooms (36sqm) with full garden views, 1 single bed, bathroom with bathtub & shower.'),
  ('00000000-0000-0000-0001-000000000002', timestamp '2026-09-15 00:00:00', timestamp '2026-09-15 00:00:00', 'Deluxe', 3000.00, 2, 'DOUBLE', 32, 2500.00, 'Rooms (36sqm) with full garden views, 1 single bed, bathroom with bathtub & shower.'),
  ('00000000-0000-0000-0001-000000000003', timestamp '2026-09-14 00:00:00', timestamp '2026-09-14 00:00:00', 'Superior', 3000.00, 2, 'DOUBLE', 32, 2500.00, 'Rooms (36sqm) with full garden views, 1 single bed, bathroom with bathtub & shower.'),
  ('00000000-0000-0000-0001-000000000004', timestamp '2026-09-13 00:00:00', timestamp '2026-09-13 00:00:00', 'Premier Sea View', 3000.00, 2, 'DOUBLE', 32, 2500.00, 'Rooms (36sqm) with full garden views, 1 single bed, bathroom with bathtub & shower.'),
  ('00000000-0000-0000-0001-000000000005', timestamp '2026-09-12 00:00:00', timestamp '2026-09-12 00:00:00', 'Supreme', 3000.00, 2, 'DOUBLE', 32, 2500.00, 'Rooms (36sqm) with full garden views, 1 single bed, bathroom with bathtub & shower.'),
  ('00000000-0000-0000-0001-000000000006', timestamp '2026-09-11 00:00:00', timestamp '2026-09-11 00:00:00', 'Suite', 3000.00, 2, 'DOUBLE', 32, 2500.00, 'Rooms (36sqm) with full garden views, 1 single bed, bathroom with bathtub & shower.')
) as seed (id, created_at, updated_at, name, price_per_night, capacity, bed_type, size_sqm, promotion_price, description)
where not exists (select 1 from room_types);

-- The same 13 amenities for every room type, in Figma order.
insert into amenities (id, created_at, updated_at, name)
select cast(a.id as uuid), timestamp '2026-09-16 00:00:00', timestamp '2026-09-16 00:00:00', a.name
from (values
  ('00000000-0000-0000-0004-000000000001', 'Safe in Room'),
  ('00000000-0000-0000-0004-000000000002', 'Air Conditioning'),
  ('00000000-0000-0000-0004-000000000003', 'High speed internet connection'),
  ('00000000-0000-0000-0004-000000000004', 'Hairdryer'),
  ('00000000-0000-0000-0004-000000000005', 'Shower'),
  ('00000000-0000-0000-0004-000000000006', 'Bathroom amenities'),
  ('00000000-0000-0000-0004-000000000007', 'Lamp'),
  ('00000000-0000-0000-0004-000000000008', 'Minibar'),
  ('00000000-0000-0000-0004-000000000009', 'Telephone'),
  ('00000000-0000-0000-0004-000000000010', 'Ironing board'),
  ('00000000-0000-0000-0004-000000000011', 'A floor only accessible via a guest room key'),
  ('00000000-0000-0000-0004-000000000012', 'Alarm clock'),
  ('00000000-0000-0000-0004-000000000013', 'Bathrobe')
) as a (id, name)
where not exists (select 1 from amenities);

insert into room_type_amenities (room_type_id, amenity_id, sort_order)
select t.id, a.id, cast(substring(cast(a.id as varchar), 25) as integer) - 1
from room_types t cross join amenities a
where t.id in (
    '00000000-0000-0000-0001-000000000001', '00000000-0000-0000-0001-000000000002', '00000000-0000-0000-0001-000000000003',
    '00000000-0000-0000-0001-000000000004', '00000000-0000-0000-0001-000000000005', '00000000-0000-0000-0001-000000000006')
  and not exists (select 1 from room_type_amenities);
