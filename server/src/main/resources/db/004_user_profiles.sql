-- Store profile data that is not managed by Clerk.
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

alter table user_profiles enable row level security;

drop policy if exists user_profiles_insert_anon on user_profiles;
create policy user_profiles_insert_anon
  on user_profiles
  for insert
  to anon
  with check (role = 'user');

-- Public profile pictures. Only the server service-role key uploads objects.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
  'profile-pictures',
  'profile-pictures',
  true,
  5242880,
  array['image/jpeg', 'image/png', 'image/webp']
)
on conflict (id) do update set
  public = excluded.public,
  file_size_limit = excluded.file_size_limit,
  allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists profile_pictures_insert_anon on storage.objects;
