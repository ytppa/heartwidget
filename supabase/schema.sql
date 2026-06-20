create extension if not exists pgcrypto;

do $$
begin
  create type public.pair_request_status as enum ('pending', 'accepted', 'rejected', 'cancelled');
exception when duplicate_object then null;
end $$;

create table if not exists public.profiles (
  user_id uuid primary key references auth.users(id) on delete cascade,
  fcm_token text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.pair_codes (
  code text primary key check (code ~ '^[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{6}$'),
  user_id uuid not null unique references auth.users(id) on delete cascade,
  active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.pair_requests (
  id uuid primary key default gen_random_uuid(),
  from_user_id uuid not null references auth.users(id) on delete cascade,
  to_user_id uuid not null references auth.users(id) on delete cascade,
  from_pair_code text not null,
  to_pair_code text not null,
  status public.pair_request_status not null default 'pending',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  responded_at timestamptz,
  check (from_user_id <> to_user_id)
);

create table if not exists public.pairs (
  id uuid primary key default gen_random_uuid(),
  user_a_id uuid not null references auth.users(id) on delete cascade,
  user_b_id uuid not null references auth.users(id) on delete cascade,
  pair_request_id uuid unique references public.pair_requests(id) on delete set null,
  created_at timestamptz not null default now(),
  check (user_a_id <> user_b_id)
);

create unique index if not exists pairs_unique_users
  on public.pairs (
    least(user_a_id::text, user_b_id::text),
    greatest(user_a_id::text, user_b_id::text)
  );

create table if not exists public.pair_member_state (
  pair_id uuid not null references public.pairs(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  partner_user_id uuid not null references auth.users(id) on delete cascade,
  sent_count integer not null default 0 check (sent_count >= 0),
  received_unread_count integer not null default 0 check (received_unread_count >= 0),
  last_sent_at timestamptz,
  last_received_at timestamptz,
  updated_at timestamptz not null default now(),
  primary key (pair_id, user_id),
  check (user_id <> partner_user_id)
);

create table if not exists public.beat_events (
  id uuid primary key default gen_random_uuid(),
  pair_id uuid not null references public.pairs(id) on delete cascade,
  from_user_id uuid not null references auth.users(id) on delete cascade,
  to_user_id uuid not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now(),
  check (from_user_id <> to_user_id)
);

alter table public.profiles enable row level security;
alter table public.pair_codes enable row level security;
alter table public.pair_requests enable row level security;
alter table public.pairs enable row level security;
alter table public.pair_member_state enable row level security;
alter table public.beat_events enable row level security;

drop policy if exists "profiles own select" on public.profiles;
create policy "profiles own select" on public.profiles
  for select to authenticated using (user_id = auth.uid());

drop policy if exists "profiles own insert" on public.profiles;
create policy "profiles own insert" on public.profiles
  for insert to authenticated with check (user_id = auth.uid());

drop policy if exists "profiles own update" on public.profiles;
create policy "profiles own update" on public.profiles
  for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());

drop policy if exists "pair codes own select" on public.pair_codes;
create policy "pair codes own select" on public.pair_codes
  for select to authenticated using (user_id = auth.uid());

drop policy if exists "pair codes own insert" on public.pair_codes;
create policy "pair codes own insert" on public.pair_codes
  for insert to authenticated with check (user_id = auth.uid());

drop policy if exists "pair codes own update" on public.pair_codes;
create policy "pair codes own update" on public.pair_codes
  for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());

drop policy if exists "pair requests participants select" on public.pair_requests;
create policy "pair requests participants select" on public.pair_requests
  for select to authenticated using (from_user_id = auth.uid() or to_user_id = auth.uid());

drop policy if exists "pairs participants select" on public.pairs;
create policy "pairs participants select" on public.pairs
  for select to authenticated using (user_a_id = auth.uid() or user_b_id = auth.uid());

drop policy if exists "pair member own select" on public.pair_member_state;
create policy "pair member own select" on public.pair_member_state
  for select to authenticated using (user_id = auth.uid());

drop policy if exists "beat events participants select" on public.beat_events;
create policy "beat events participants select" on public.beat_events
  for select to authenticated using (from_user_id = auth.uid() or to_user_id = auth.uid());

create or replace function public.touch_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists touch_profiles_updated_at on public.profiles;
create trigger touch_profiles_updated_at
before update on public.profiles
for each row execute function public.touch_updated_at();

drop trigger if exists touch_pair_codes_updated_at on public.pair_codes;
create trigger touch_pair_codes_updated_at
before update on public.pair_codes
for each row execute function public.touch_updated_at();

drop trigger if exists touch_pair_requests_updated_at on public.pair_requests;
create trigger touch_pair_requests_updated_at
before update on public.pair_requests
for each row execute function public.touch_updated_at();

create or replace function public.normalize_pair_code(p_code text)
returns text
language sql
immutable
as $$
  select upper(regexp_replace(coalesce(p_code, ''), '[^0-9A-Za-z]', '', 'g'));
$$;

create or replace function public.require_auth_uid()
returns uuid
language plpgsql
stable
as $$
declare
  v_uid uuid := auth.uid();
begin
  if v_uid is null then
    raise exception 'Authentication required' using errcode = '28000';
  end if;

  return v_uid;
end;
$$;

create or replace function public.generate_pair_code_value()
returns text
language plpgsql
as $$
declare
  v_alphabet text := '23456789ABCDEFGHJKLMNPQRSTUVWXYZ';
  v_bytes bytea := extensions.gen_random_bytes(6);
  v_code text := '';
  i integer;
begin
  for i in 0..5 loop
    v_code := v_code || substr(v_alphabet, (get_byte(v_bytes, i) % length(v_alphabet)) + 1, 1);
  end loop;

  return v_code;
end;
$$;

create or replace function public.ensure_profile(p_fcm_token text default null)
returns public.profiles
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid uuid := public.require_auth_uid();
  v_profile public.profiles;
begin
  insert into public.profiles (user_id, fcm_token)
  values (v_uid, nullif(p_fcm_token, ''))
  on conflict (user_id) do update
    set fcm_token = coalesce(nullif(excluded.fcm_token, ''), public.profiles.fcm_token),
        updated_at = now()
  returning * into v_profile;

  return v_profile;
end;
$$;

create or replace function public.create_pair_code()
returns text
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid uuid := public.require_auth_uid();
  v_code text;
  v_existing_code text;
  v_attempts integer := 0;
begin
  perform public.ensure_profile(null::text);

  select pc.code into v_existing_code
  from public.pair_codes pc
  where pc.user_id = v_uid and pc.active = true
  limit 1;

  if v_existing_code is not null then
    return v_existing_code;
  end if;

  loop
    v_attempts := v_attempts + 1;
    if v_attempts > 20 then
      raise exception 'Could not generate pair code';
    end if;

    v_code := public.generate_pair_code_value();

    begin
      insert into public.pair_codes (code, user_id, active)
      values (v_code, v_uid, true)
      on conflict (user_id) do update
        set code = excluded.code,
            active = true,
            updated_at = now()
      returning code into v_existing_code;

      return v_existing_code;
    exception when unique_violation then
      null;
    end;
  end loop;
end;
$$;

create or replace function public.request_pairing(p_to_pair_code text)
returns public.pair_requests
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid uuid := public.require_auth_uid();
  v_to_pair_code text := public.normalize_pair_code(p_to_pair_code);
  v_from_pair_code text;
  v_to_user_id uuid;
  v_request public.pair_requests;
begin
  if v_to_pair_code !~ '^[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{6}$' then
    raise exception 'Invalid pair code';
  end if;

  perform public.ensure_profile(null::text);
  v_from_pair_code := public.create_pair_code();

  select pc.user_id into v_to_user_id
  from public.pair_codes pc
  where pc.code = v_to_pair_code and pc.active = true;

  if v_to_user_id is null then
    raise exception 'Pair code not found';
  end if;

  if v_to_user_id = v_uid then
    raise exception 'Cannot pair with own code';
  end if;

  insert into public.pair_requests (
    from_user_id,
    to_user_id,
    from_pair_code,
    to_pair_code,
    status
  )
  values (
    v_uid,
    v_to_user_id,
    v_from_pair_code,
    v_to_pair_code,
    'pending'
  )
  returning * into v_request;

  return v_request;
end;
$$;

create or replace function public.accept_pairing(p_request_id uuid)
returns public.pairs
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid uuid := public.require_auth_uid();
  v_request public.pair_requests;
  v_pair public.pairs;
begin
  select * into v_request
  from public.pair_requests
  where id = p_request_id
  for update;

  if v_request.id is null then
    raise exception 'Pair request not found';
  end if;

  if v_request.to_user_id <> v_uid then
    raise exception 'Only the recipient can accept this request';
  end if;

  if v_request.status <> 'pending' then
    raise exception 'Pair request is not pending';
  end if;

  update public.pair_requests
  set status = 'accepted',
      responded_at = now(),
      updated_at = now()
  where id = p_request_id
  returning * into v_request;

  insert into public.pairs (
    user_a_id,
    user_b_id,
    pair_request_id
  )
  values (
    v_request.from_user_id,
    v_request.to_user_id,
    v_request.id
  )
  on conflict (pair_request_id) do update
    set pair_request_id = excluded.pair_request_id
  returning * into v_pair;

  insert into public.pair_member_state (
    pair_id,
    user_id,
    partner_user_id
  )
  values
    (v_pair.id, v_request.from_user_id, v_request.to_user_id),
    (v_pair.id, v_request.to_user_id, v_request.from_user_id)
  on conflict (pair_id, user_id) do nothing;

  return v_pair;
end;
$$;

create or replace function public.get_my_pairing_state()
returns table (
  pair_code text,
  pair_id uuid,
  partner_user_id uuid,
  partner_pair_code text,
  pair_request_id uuid,
  pair_status text,
  sent_count integer,
  received_unread_count integer
)
language sql
security definer
set search_path = public
as $$
  with me as (
    select public.require_auth_uid() as user_id
  ),
  my_code as (
    select pc.code
    from public.pair_codes pc, me
    where pc.user_id = me.user_id and pc.active = true
    limit 1
  ),
  my_pair as (
    select
      p.id as pair_id,
      case when p.user_a_id = me.user_id then p.user_b_id else p.user_a_id end as partner_user_id,
      p.pair_request_id
    from public.pairs p, me
    where p.user_a_id = me.user_id or p.user_b_id = me.user_id
    order by p.created_at desc
    limit 1
  ),
  pending_outgoing as (
    select pr.id, pr.to_user_id, pr.to_pair_code
    from public.pair_requests pr, me
    where pr.from_user_id = me.user_id and pr.status = 'pending'
    order by pr.created_at desc
    limit 1
  ),
  pending_incoming as (
    select pr.id, pr.from_user_id, pr.from_pair_code
    from public.pair_requests pr, me
    where pr.to_user_id = me.user_id and pr.status = 'pending'
    order by pr.created_at desc
    limit 1
  )
  select
    (select code from my_code) as pair_code,
    my_pair.pair_id,
    coalesce(my_pair.partner_user_id, pending_incoming.from_user_id, pending_outgoing.to_user_id) as partner_user_id,
    coalesce(
      partner_code.code,
      pending_incoming.from_pair_code,
      pending_outgoing.to_pair_code
    ) as partner_pair_code,
    coalesce(my_pair.pair_request_id, pending_incoming.id, pending_outgoing.id) as pair_request_id,
    case
      when my_pair.pair_id is not null then 'paired'
      when pending_incoming.id is not null then 'pending_incoming'
      when pending_outgoing.id is not null then 'pending_outgoing'
      else 'none'
    end as pair_status,
    coalesce(member_state.sent_count, 0) as sent_count,
    coalesce(member_state.received_unread_count, 0) as received_unread_count
  from me
  left join my_pair on true
  left join pending_incoming on my_pair.pair_id is null
  left join pending_outgoing on my_pair.pair_id is null and pending_incoming.id is null
  left join public.pair_codes partner_code on partner_code.user_id = my_pair.partner_user_id and partner_code.active = true
  left join public.pair_member_state member_state on member_state.pair_id = my_pair.pair_id and member_state.user_id = me.user_id;
$$;

create or replace function public.get_active_pair_for_user(p_user_id uuid)
returns public.pairs
language plpgsql
stable
security definer
set search_path = public
as $$
declare
  v_uid uuid := public.require_auth_uid();
  v_pair public.pairs;
begin
  if p_user_id <> v_uid then
    raise exception 'Cannot read another user pair';
  end if;

  select * into v_pair
  from public.pairs p
  where p.user_a_id = p_user_id or p.user_b_id = p_user_id
  order by p.created_at desc
  limit 1;

  if v_pair.id is null then
    raise exception 'No active pair';
  end if;

  return v_pair;
end;
$$;

create or replace function public.send_beat()
returns table (
  pair_id uuid,
  from_user_id uuid,
  to_user_id uuid,
  sent_count integer,
  partner_received_unread_count integer
)
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid uuid := public.require_auth_uid();
  v_pair public.pairs;
  v_partner_user_id uuid;
  v_sent_count integer;
  v_partner_received_count integer;
begin
  v_pair := public.get_active_pair_for_user(v_uid);
  v_partner_user_id := case
    when v_pair.user_a_id = v_uid then v_pair.user_b_id
    else v_pair.user_a_id
  end;

  insert into public.beat_events (pair_id, from_user_id, to_user_id)
  values (v_pair.id, v_uid, v_partner_user_id);

  update public.pair_member_state
  set sent_count = sent_count + 1,
      last_sent_at = now(),
      updated_at = now()
  where pair_member_state.pair_id = v_pair.id
    and pair_member_state.user_id = v_uid
  returning pair_member_state.sent_count into v_sent_count;

  update public.pair_member_state
  set received_unread_count = received_unread_count + 1,
      last_received_at = now(),
      updated_at = now()
  where pair_member_state.pair_id = v_pair.id
    and pair_member_state.user_id = v_partner_user_id
  returning pair_member_state.received_unread_count into v_partner_received_count;

  return query select
    v_pair.id,
    v_uid,
    v_partner_user_id,
    v_sent_count,
    v_partner_received_count;
end;
$$;

create or replace function public.clear_received_beats()
returns table (
  pair_id uuid,
  user_id uuid,
  received_unread_count integer
)
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid uuid := public.require_auth_uid();
  v_pair public.pairs;
begin
  v_pair := public.get_active_pair_for_user(v_uid);

  update public.pair_member_state
  set received_unread_count = 0,
      updated_at = now()
  where pair_member_state.pair_id = v_pair.id
    and pair_member_state.user_id = v_uid;

  return query select
    v_pair.id,
    v_uid,
    0;
end;
$$;

revoke execute on function public.touch_updated_at() from public, anon, authenticated;
revoke execute on function public.normalize_pair_code(text) from public, anon, authenticated;
revoke execute on function public.require_auth_uid() from public, anon, authenticated;
revoke execute on function public.generate_pair_code_value() from public, anon, authenticated;
revoke execute on function public.get_active_pair_for_user(uuid) from public, anon, authenticated;

revoke execute on function public.ensure_profile(text) from public, anon;
revoke execute on function public.create_pair_code() from public, anon;
revoke execute on function public.request_pairing(text) from public, anon;
revoke execute on function public.accept_pairing(uuid) from public, anon;
revoke execute on function public.get_my_pairing_state() from public, anon;
revoke execute on function public.send_beat() from public, anon;
revoke execute on function public.clear_received_beats() from public, anon;

grant execute on function public.ensure_profile(text) to authenticated;
grant execute on function public.create_pair_code() to authenticated;
grant execute on function public.request_pairing(text) to authenticated;
grant execute on function public.accept_pairing(uuid) to authenticated;
grant execute on function public.get_my_pairing_state() to authenticated;
grant execute on function public.send_beat() to authenticated;
grant execute on function public.clear_received_beats() to authenticated;
