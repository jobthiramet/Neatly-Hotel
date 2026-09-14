-- Run in Supabase SQL Editor after 001_hotel_info.sql.
-- Also loaded by the local (H2) profile, so keep it portable SQL.
-- logo_url starts empty: upload the logo from admin / hotel information.

insert into hotel_info (id, created_at, updated_at, name, description, logo_url)
select
  '00000000-0000-0000-0000-000000000001',
  current_timestamp,
  current_timestamp,
  'Neatly Hotel',
  'Set in Bangkok, Thailand. Neatly Hotel offers 5-star accommodation with an outdoor pool, kids'' club, sports facilities and a fitness centre. There is also a spa, an indoor pool and saunas.' || chr(10) || chr(10) ||
  'All units at the hotel are equipped with a seating area, a flat-screen TV with satellite channels, a dining area and a private bathroom with free toiletries, a bathtub and a hairdryer. Every room in Neatly Hotel features a furnished balcony. Some rooms are equipped with a coffee machine.' || chr(10) || chr(10) ||
  'Free WiFi and entertainment facilities are available at property and also rentals are provided to explore the area.',
  null
where not exists (select 1 from hotel_info);
