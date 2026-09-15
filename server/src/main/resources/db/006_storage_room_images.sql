-- Run in Supabase SQL Editor after 005_rooms.sql.
-- Public bucket for room photos. Anyone can read objects via the public URL.
-- No policies on storage.objects: only the server (service role key) can upload or delete.

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('room-images', 'room-images', true, 5242880, array['image/png', 'image/jpeg', 'image/webp'])
on conflict (id) do update set
  public = excluded.public,
  file_size_limit = excluded.file_size_limit,
  allowed_mime_types = excluded.allowed_mime_types;
