-- Run in Supabase SQL Editor after 013_promotion_code_rules.sql.
-- Single-row guest chatbot script. The local profile loads 014_chatbot_script_seed.sql only;
-- this file also creates the table for Postgres.

create table if not exists chatbot_script (
  id uuid primary key,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  greeting text not null,
  auto_reply text not null,
  topics text not null
);

create unique index if not exists chatbot_script_singleton on chatbot_script ((true));

alter table chatbot_script enable row level security;
