-- Run in Supabase SQL Editor after 008_room_cleanup_and_amenities.sql and after the six room types
-- are seeded (seed-rooms.ps1). Also runs in the local (H2) profile. Safe to run more than once.
-- Committed earlier as 008 (VACANT status, no floor); if that version already ran, 008_room_cleanup_and_amenities.sql upgraded it.

-- Housekeeping status only. Vacant/Occupied comes from bookings (see room_unit_occupancy in 010).
-- Labels follow the design-system Badge: "Vacant" + "Clean" = "Vacant Clean"; ASSIGN_* and OUT_OF_* show alone.
create table if not exists room_statuses (
  id uuid primary key,
  created_at timestamp with time zone not null,
  updated_at timestamp with time zone not null,
  code varchar(50) not null unique,
  label varchar(100) not null,
  sort_order integer not null
);

-- ponytail: room_number is unique across deleted units too; a partial index would allow reuse but H2 lacks it.
create table if not exists room_units (
  id uuid primary key,
  created_at timestamp with time zone not null,
  updated_at timestamp with time zone not null,
  room_number varchar(10) not null unique,
  floor smallint not null,
  room_type_id uuid not null references room_types (id) on delete restrict,
  room_status_id uuid not null references room_statuses (id) on delete restrict,
  deleted_at timestamp with time zone,
  constraint room_units_room_number_format check (
    regexp_like(room_number, '^[0-9]{4}$')),
  constraint room_units_floor_check check (floor > 0)
);

create index if not exists room_units_room_type_id_idx on room_units (room_type_id);
create index if not exists room_units_room_status_id_idx on room_units (room_status_id);

insert into room_statuses (id, created_at, updated_at, code, label, sort_order)
select cast(v.id as uuid), now(), now(), v.code, v.label, v.sort_order
from (values
  ('00000000-0000-0000-0002-000000000002', 'ASSIGN_CLEAN', 'Assign Clean', 1),
  ('00000000-0000-0000-0002-000000000003', 'ASSIGN_DIRTY', 'Assign Dirty', 2),
  ('00000000-0000-0000-0002-000000000001', 'CLEAN', 'Clean', 3),
  ('00000000-0000-0000-0002-000000000004', 'CLEAN_INSPECTED', 'Clean Inspected', 4),
  ('00000000-0000-0000-0002-000000000005', 'CLEAN_PICK_UP', 'Clean Pick Up', 5),
  ('00000000-0000-0000-0002-000000000006', 'DIRTY', 'Dirty', 6),
  ('00000000-0000-0000-0002-000000000007', 'OUT_OF_ORDER', 'Out of Order', 7),
  ('00000000-0000-0000-0002-000000000008', 'OUT_OF_SERVICE', 'Out of Service', 8)
) as v (id, code, label, sort_order)
where not exists (select 1 from room_statuses s where s.code = v.code or s.id = cast(v.id as uuid));

-- 40 units, cheaper types first, 10 per floor; ids end in the room number. All start CLEAN (shown as Vacant Clean).
-- Types are matched by name among non-deleted room types.
insert into room_units (id, created_at, updated_at, room_number, floor, room_type_id, room_status_id)
select cast('00000000-0000-0000-0003-00000000' || v.room_number as uuid), now(), now(), v.room_number,
  cast((cast(v.room_number as integer) - 1) / 10 + 1 as smallint), t.id, s.id
from (values
  ('0001', 'Superior'), ('0002', 'Superior'), ('0003', 'Superior'), ('0004', 'Superior'), ('0005', 'Superior'),
  ('0006', 'Superior'), ('0007', 'Superior'), ('0008', 'Superior'), ('0009', 'Superior'), ('0010', 'Superior'),
  ('0011', 'Deluxe'), ('0012', 'Deluxe'), ('0013', 'Deluxe'), ('0014', 'Deluxe'),
  ('0015', 'Deluxe'), ('0016', 'Deluxe'), ('0017', 'Deluxe'), ('0018', 'Deluxe'),
  ('0019', 'Superior Garden View'), ('0020', 'Superior Garden View'), ('0021', 'Superior Garden View'), ('0022', 'Superior Garden View'),
  ('0023', 'Superior Garden View'), ('0024', 'Superior Garden View'), ('0025', 'Superior Garden View'), ('0026', 'Superior Garden View'),
  ('0027', 'Premier Sea View'), ('0028', 'Premier Sea View'), ('0029', 'Premier Sea View'),
  ('0030', 'Premier Sea View'), ('0031', 'Premier Sea View'), ('0032', 'Premier Sea View'),
  ('0033', 'Supreme'), ('0034', 'Supreme'), ('0035', 'Supreme'), ('0036', 'Supreme'), ('0037', 'Supreme'),
  ('0038', 'Suite'), ('0039', 'Suite'), ('0040', 'Suite')
) as v (room_number, type_name)
join room_types t on t.name = v.type_name and t.deleted_at is null
join room_statuses s on s.code = 'CLEAN'
where not exists (select 1 from room_units u where u.room_number = v.room_number);
