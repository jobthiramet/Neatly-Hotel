-- Run in Supabase SQL Editor after 007_rename_rooms_to_room_types.sql and after the six room types
-- are seeded (seed-rooms.ps1). Also runs in the local (H2) profile. Safe to run more than once.

create table if not exists room_statuses (
  id uuid primary key,
  created_at timestamp with time zone not null,
  updated_at timestamp with time zone not null,
  code varchar(50) not null unique,
  label varchar(100) not null,
  sort_order integer not null
);

-- Room units have no soft delete yet. Room types are soft-deleted, so restrict hard deletes.
create table if not exists room_units (
  id uuid primary key,
  created_at timestamp with time zone not null,
  updated_at timestamp with time zone not null,
  room_number varchar(10) not null unique,
  room_type_id uuid not null references room_types (id) on delete restrict,
  room_status_id uuid not null references room_statuses (id) on delete restrict
);

create index if not exists room_units_room_type_id_idx on room_units (room_type_id);
create index if not exists room_units_room_status_id_idx on room_units (room_status_id);

-- Only VACANT for now; the full list comes from the Figma Room Management frames.
insert into room_statuses (id, created_at, updated_at, code, label, sort_order)
select cast('00000000-0000-0000-0002-000000000001' as uuid), now(), now(), 'VACANT', 'Vacant', 1
where not exists (select 1 from room_statuses where code = 'VACANT');

-- 40 units, cheaper types first; ids end in the room number. Types are matched by name among non-deleted room types.
insert into room_units (id, created_at, updated_at, room_number, room_type_id, room_status_id)
select cast('00000000-0000-0000-0003-00000000' || v.room_number as uuid), now(), now(), v.room_number, t.id, s.id
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
join room_statuses s on s.code = 'VACANT'
where not exists (select 1 from room_units u where u.room_number = v.room_number);
