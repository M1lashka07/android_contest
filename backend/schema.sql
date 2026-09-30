-- Contest application schema, 30 September 2026.
-- Only contest_* objects, one private schema and a dedicated avatar bucket are added.
-- Existing profiles/games/participants/game_moves/messages tables are untouched.
begin;

create schema if not exists contest_internal;
revoke all on schema contest_internal from public, anon;
grant usage on schema contest_internal to authenticated;

create table if not exists public.contest_profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  name text not null check (char_length(name) between 2 and 80),
  username text not null default '' check (char_length(username) <= 80),
  location text not null default '' check (char_length(location) <= 120),
  avatar_url text,
  points integer not null default 0 check (points >= 0),
  created_at timestamptz not null default now()
);
create table if not exists public.contest_games (
  id uuid primary key default gen_random_uuid(),
  title text not null check (char_length(title) between 3 and 80),
  category text not null check (category in ('IMAGE','CIRCLE')),
  host_id uuid not null references public.contest_profiles(id),
  guest_id uuid references public.contest_profiles(id),
  starts_at timestamptz not null,
  ends_at timestamptz,
  winning_points integer not null default 100 check (winning_points between 0 and 1000),
  description text not null default '' check (char_length(description) <= 2000),
  image_seed integer not null check (image_seed between 1 and 2147483646),
  status text not null default 'SCHEDULED' check (status in ('SCHEDULED','ACTIVE','FINISHED')),
  started_at timestamptz,
  finished_at timestamptz,
  winner_id uuid references public.contest_profiles(id),
  created_at timestamptz not null default now(),
  check (guest_id is null or guest_id <> host_id),
  check (ends_at is null or ends_at > starts_at),
  check (winner_id is null or winner_id = host_id or winner_id = guest_id)
);
create table if not exists public.contest_participants (
  game_id uuid not null references public.contest_games(id) on delete cascade,
  user_id uuid not null references public.contest_profiles(id) on delete cascade,
  last_seen_at timestamptz not null default now(),
  elapsed_ms bigint,
  replay integer[],
  result text check (result in ('WIN','LOSS')),
  forfeited_at timestamptz,
  primary key (game_id,user_id)
);
create index if not exists contest_games_starts_at_idx on public.contest_games(starts_at desc);
create index if not exists contest_games_host_created_idx on public.contest_games(host_id,created_at);
create index if not exists contest_games_winner_finished_idx on public.contest_games(winner_id,finished_at) where winner_id is not null;
create index if not exists contest_games_guest_idx on public.contest_games(guest_id) where guest_id is not null;
create index if not exists contest_participants_user_idx on public.contest_participants(user_id);
create index if not exists contest_profiles_points_idx on public.contest_profiles(points desc);

alter table public.contest_profiles enable row level security;
alter table public.contest_games enable row level security;
alter table public.contest_participants enable row level security;
revoke all on public.contest_profiles, public.contest_games, public.contest_participants from anon, authenticated;
grant select on public.contest_profiles, public.contest_games to authenticated;
grant select on public.contest_participants to authenticated;
grant update(name,location,avatar_url,username) on public.contest_profiles to authenticated;
drop policy if exists contest_profiles_read on public.contest_profiles;
create policy contest_profiles_read on public.contest_profiles for select to authenticated using (true);
drop policy if exists contest_profiles_edit_self on public.contest_profiles;
create policy contest_profiles_edit_self on public.contest_profiles for update to authenticated
  using ((select auth.uid()) = id) with check ((select auth.uid()) = id);
drop policy if exists contest_games_read on public.contest_games;
create policy contest_games_read on public.contest_games for select to authenticated using (true);
drop policy if exists contest_participants_read_self on public.contest_participants;
create policy contest_participants_read_self on public.contest_participants for select to authenticated
  using ((select auth.uid()) = user_id);

-- Helpers have no public EXECUTE grant. Mutations use narrow authenticated functions.
create or replace function contest_internal.initial_board(p_seed integer)
returns integer[] language plpgsql immutable set search_path = '' as $$
declare
  b integer[] := array[1,2,3,4,5,6,7,8,0];
  r bigint := greatest(p_seed,1); empty_pos integer := 8; previous_pos integer := -1;
  candidates integer[]; next_pos integer; step integer; i integer;
begin
  for step in 1..80 loop
    candidates := array[]::integer[];
    for i in 0..8 loop
      if i <> previous_pos and abs(i / 3 - empty_pos / 3) + abs(i % 3 - empty_pos % 3) = 1 then
        candidates := array_append(candidates,i);
      end if;
    end loop;
    r := (r * 48271) % 2147483647;
    next_pos := candidates[1 + (r % cardinality(candidates))::integer];
    b[empty_pos+1] := b[next_pos+1]; b[next_pos+1] := 0;
    previous_pos := empty_pos; empty_pos := next_pos;
  end loop;
  if b = array[1,2,3,4,5,6,7,8,0] then b[9] := b[8]; b[8] := 0; end if;
  return b;
end $$;

create or replace function contest_internal.ensure_profile()
returns jsonb language plpgsql security definer set search_path = '' as $$
declare actor uuid := auth.uid(); metadata jsonb; display_name text; row_data public.contest_profiles;
begin
  if actor is null then raise exception 'Требуется авторизация' using errcode = '28000'; end if;
  select raw_user_meta_data into metadata from auth.users where id = actor;
  -- Metadata is used only for display, never for permissions or ownership.
  display_name := left(coalesce(nullif(btrim(metadata->>'name'),''),'Player'),80);
  if char_length(display_name) < 2 then display_name := 'Player'; end if;
  insert into public.contest_profiles(id,name,username)
    values(actor,display_name,left(coalesce(metadata->>'username',''),80)) on conflict(id) do nothing;
  select * into row_data from public.contest_profiles where id = actor;
  return to_jsonb(row_data);
end $$;

create or replace function contest_internal.update_profile(p_name text,p_location text,p_avatar_url text)
returns jsonb language plpgsql security definer set search_path='' as $$
declare actor uuid:=auth.uid(); row_data public.contest_profiles;
begin
  if actor is null then raise exception 'Требуется авторизация' using errcode='28000'; end if;
  if p_name is null or char_length(btrim(p_name)) not between 2 and 80 then raise exception 'Имя: от 2 до 80 символов'; end if;
  if char_length(coalesce(p_location,''))>120 then raise exception 'Местоположение слишком длинное'; end if;
  if p_avatar_url is not null and (char_length(p_avatar_url)>2048 or p_avatar_url not like 'https://%') then raise exception 'Некорректный адрес фотографии'; end if;
  perform contest_internal.ensure_profile();
  update public.contest_profiles set name=btrim(p_name),location=coalesce(p_location,''),avatar_url=coalesce(p_avatar_url,avatar_url)
    where id=actor returning * into row_data;
  return to_jsonb(row_data);
end $$;

create or replace function contest_internal.create_game(p_title text,p_category text,p_starts_at timestamptz,
    p_ends_at timestamptz,p_winning_points integer,p_description text)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare actor uuid := auth.uid(); g public.contest_games;
begin
  if actor is null then raise exception 'Требуется авторизация' using errcode = '28000'; end if;
  if char_length(btrim(p_title)) not between 3 and 80 then raise exception 'Название: от 3 до 80 символов'; end if;
  if p_category not in ('IMAGE','CIRCLE') then raise exception 'Неизвестная категория'; end if;
  if p_starts_at < clock_timestamp() - interval '1 minute' then raise exception 'Выберите время в будущем'; end if;
  if p_ends_at is not null and p_ends_at <= p_starts_at then raise exception 'Окончание должно быть позже начала'; end if;
  if p_winning_points not between 0 and 1000 then raise exception 'Приз должен быть от 0 до 1000 баллов'; end if;
  perform contest_internal.ensure_profile();
  insert into public.contest_games(title,category,host_id,starts_at,ends_at,winning_points,description,image_seed)
    values(btrim(p_title),p_category,actor,p_starts_at,p_ends_at,p_winning_points,coalesce(p_description,''),
      1+floor(random()*2147483646)::integer) returning * into g;
  insert into public.contest_participants(game_id,user_id) values(g.id,actor);
  return to_jsonb(g);
end $$;

create or replace function contest_internal.join_game(p_game_id uuid)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare actor uuid := auth.uid(); g public.contest_games;
begin
  if actor is null then raise exception 'Требуется авторизация' using errcode = '28000'; end if;
  select * into g from public.contest_games where id = p_game_id for update;
  if not found then raise exception 'Игра не найдена'; end if;
  if actor = g.host_id or actor = g.guest_id then return to_jsonb(g); end if;
  if g.status <> 'SCHEDULED' or g.guest_id is not null then raise exception 'В этой игре нет свободного места'; end if;
  if g.ends_at is not null and clock_timestamp() >= g.ends_at then raise exception 'Время этой игры истекло'; end if;
  perform contest_internal.ensure_profile();
  update public.contest_games set guest_id = actor where id = g.id returning * into g;
  insert into public.contest_participants(game_id,user_id) values(g.id,actor);
  return to_jsonb(g);
end $$;

create or replace function contest_internal.finish_game(p_game_id uuid,p_winner uuid)
returns public.contest_games language plpgsql security definer set search_path = '' as $$
declare g public.contest_games;
begin
  select * into g from public.contest_games where id = p_game_id for update;
  if g.status = 'FINISHED' then return g; end if;
  if p_winner is not null and p_winner <> g.host_id and p_winner is distinct from g.guest_id then
    raise exception 'Победитель не является участником';
  end if;
  update public.contest_games set status='FINISHED',finished_at=clock_timestamp(),winner_id=p_winner
    where id = p_game_id returning * into g;
  update public.contest_participants set result=case when user_id=p_winner then 'WIN' else 'LOSS' end where game_id=g.id;
  if p_winner is not null then
    update public.contest_profiles set points=points+g.winning_points where id=p_winner;
  end if;
  return g;
end $$;

create or replace function contest_internal.get_game(p_game_id uuid)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare actor uuid := auth.uid(); g public.contest_games; host_seen timestamptz; guest_seen timestamptz; cutoff timestamptz;
begin
  if actor is null then raise exception 'Требуется авторизация' using errcode = '28000'; end if;
  select * into g from public.contest_games where id=p_game_id for update;
  if not found then raise exception 'Игра не найдена'; end if;
  if g.status='ACTIVE' then
    select last_seen_at into host_seen from public.contest_participants where game_id=g.id and user_id=g.host_id;
    select last_seen_at into guest_seen from public.contest_participants where game_id=g.id and user_id=g.guest_id;
    cutoff := clock_timestamp() - interval '45 seconds';
    -- Expiration is evaluated BEFORE refreshing the caller; a departed player cannot revive its stale heartbeat.
    if host_seen < cutoff and guest_seen < cutoff then
      g := contest_internal.finish_game(g.id,null);
    elsif host_seen < cutoff then
      g := contest_internal.finish_game(g.id,g.guest_id);
    elsif guest_seen < cutoff then
      g := contest_internal.finish_game(g.id,g.host_id);
    elsif g.ends_at is not null and clock_timestamp() >= g.ends_at then
      g := contest_internal.finish_game(g.id,null);
    end if;
  end if;
  if actor=g.host_id or actor=g.guest_id then
    update public.contest_participants set last_seen_at=clock_timestamp() where game_id=g.id and user_id=actor;
  end if;
  return to_jsonb(g);
end $$;

create or replace function contest_internal.start_game(p_game_id uuid)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare actor uuid := auth.uid(); g public.contest_games;
begin
  if actor is null then raise exception 'Требуется авторизация' using errcode = '28000'; end if;
  select * into g from public.contest_games where id=p_game_id for update;
  if not found then raise exception 'Игра не найдена'; end if;
  if actor <> g.host_id and actor is distinct from g.guest_id then raise exception 'Вы не участник этой игры'; end if;
  if g.status <> 'SCHEDULED' then return contest_internal.get_game(g.id); end if;
  if g.guest_id is null then raise exception 'Дождитесь соперника'; end if;
  if clock_timestamp() < g.starts_at then raise exception 'Игра ещё не началась'; end if;
  if g.ends_at is not null and clock_timestamp() >= g.ends_at then raise exception 'Время игры истекло'; end if;
  update public.contest_games set status='ACTIVE',started_at=clock_timestamp() where id=g.id returning * into g;
  update public.contest_participants set last_seen_at=g.started_at where game_id=g.id;
  return to_jsonb(g);
end $$;

create or replace function contest_internal.save_result(p_game_id uuid,p_moves integer[])
returns jsonb language plpgsql security definer set search_path = '' as $$
declare actor uuid := auth.uid(); g public.contest_games; b integer[]; empty_pos integer; tile integer;
begin
  if actor is null then raise exception 'Требуется авторизация' using errcode = '28000'; end if;
  select * into g from public.contest_games where id=p_game_id for update;
  if not found then raise exception 'Игра не найдена'; end if;
  if actor <> g.host_id and actor is distinct from g.guest_id then raise exception 'Вы не участник этой игры'; end if;
  perform contest_internal.get_game(g.id);
  select * into g from public.contest_games where id=p_game_id;
  if g.status='FINISHED' then return to_jsonb(g); end if;
  if g.status <> 'ACTIVE' or g.started_at is null then raise exception 'Игра не запущена'; end if;
  if p_moves is null or cardinality(p_moves) not between 1 and 10000 then raise exception 'Некорректный список ходов'; end if;
  if g.category='CIRCLE' then
    if p_moves <> array[5,4,3,2,1] then raise exception 'Круги должны идти от большего к меньшему'; end if;
  else
    b := contest_internal.initial_board(g.image_seed);
    foreach tile in array p_moves loop
      empty_pos := array_position(b,0)-1;
      if tile is null or tile not between 0 and 8 or abs(tile/3-empty_pos/3)+abs(tile%3-empty_pos%3) <> 1 then
        raise exception 'Недопустимый ход пазла';
      end if;
      b[empty_pos+1] := b[tile+1]; b[tile+1] := 0;
    end loop;
    if b <> array[1,2,3,4,5,6,7,8,0] then raise exception 'Пазл ещё не собран'; end if;
  end if;
  update public.contest_participants set replay=p_moves,
    elapsed_ms=greatest(0,floor(extract(epoch from (clock_timestamp()-g.started_at))*1000)::bigint)
    where game_id=g.id and user_id=actor;
  -- Row locking makes the first valid server-received completion the sole winner.
  g := contest_internal.finish_game(g.id,actor);
  return to_jsonb(g);
end $$;

create or replace function contest_internal.forfeit_game(p_game_id uuid)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare actor uuid := auth.uid(); g public.contest_games; opponent uuid;
begin
  if actor is null then raise exception 'Требуется авторизация' using errcode = '28000'; end if;
  select * into g from public.contest_games where id=p_game_id for update;
  if not found then raise exception 'Игра не найдена'; end if;
  if actor <> g.host_id and actor is distinct from g.guest_id then raise exception 'Вы не участник этой игры'; end if;
  if g.status='FINISHED' then return to_jsonb(g); end if;
  if g.status <> 'ACTIVE' then raise exception 'Поражение можно зафиксировать только в начатой игре'; end if;
  opponent := case when actor=g.host_id then g.guest_id else g.host_id end;
  update public.contest_participants set forfeited_at=clock_timestamp() where game_id=g.id and user_id=actor;
  g := contest_internal.finish_game(g.id,opponent);
  return to_jsonb(g);
end $$;

create or replace function contest_internal.statistics()
returns jsonb language plpgsql security definer set search_path = '' as $$
declare actor uuid := auth.uid(); week_start timestamptz; week_end timestamptz; daily jsonb; result jsonb;
begin
  if actor is null then raise exception 'Требуется авторизация' using errcode = '28000'; end if;
  week_start := date_trunc('week',clock_timestamp() at time zone 'Europe/Moscow') at time zone 'Europe/Moscow';
  week_end := week_start+interval '7 days';
  select jsonb_agg(coalesce(t.points,0) order by d.day) into daily from generate_series(0,6) d(day)
    left join lateral (select sum(winning_points)::integer points from public.contest_games
      where winner_id=actor and finished_at>=week_start+d.day*interval '1 day'
        and finished_at<week_start+(d.day+1)*interval '1 day') t on true;
  select jsonb_build_object(
    'weekly_points',coalesce(sum(winning_points) filter(where winner_id=actor and finished_at>=week_start and finished_at<week_end),0),
    'image_wins',count(*) filter(where winner_id=actor and category='IMAGE'),
    'circle_wins',count(*) filter(where winner_id=actor and category='CIRCLE'),
    'scheduled_this_week',count(*) filter(where host_id=actor and created_at>=week_start and created_at<week_end),
    'daily_points',daily) into result from public.contest_games where host_id=actor or guest_id=actor;
  return result;
end $$;

-- Public API wrappers remain SECURITY INVOKER. Only controlled private entry points are callable.
create or replace function public.contest_ensure_profile() returns jsonb language sql security invoker set search_path='' as $$ select contest_internal.ensure_profile(); $$;
create or replace function public.contest_update_profile(p_name text,p_location text,p_avatar_url text default null)
returns jsonb language sql security invoker set search_path='' as $$ select contest_internal.update_profile(p_name,p_location,p_avatar_url); $$;
create or replace function public.contest_create_game(p_title text,p_category text,p_starts_at timestamptz,p_ends_at timestamptz default null,p_winning_points integer default 100,p_description text default '')
 returns jsonb language sql security invoker set search_path='' as $$ select contest_internal.create_game(p_title,p_category,p_starts_at,p_ends_at,p_winning_points,p_description); $$;
create or replace function public.contest_join_game(p_game_id uuid) returns jsonb language sql security invoker set search_path='' as $$ select contest_internal.join_game(p_game_id); $$;
create or replace function public.contest_get_game(p_game_id uuid) returns jsonb language sql security invoker set search_path='' as $$ select contest_internal.get_game(p_game_id); $$;
create or replace function public.contest_start_game(p_game_id uuid) returns jsonb language sql security invoker set search_path='' as $$ select contest_internal.start_game(p_game_id); $$;
create or replace function public.contest_save_result(p_game_id uuid,p_moves integer[]) returns jsonb language sql security invoker set search_path='' as $$ select contest_internal.save_result(p_game_id,p_moves); $$;
create or replace function public.contest_forfeit_game(p_game_id uuid) returns jsonb language sql security invoker set search_path='' as $$ select contest_internal.forfeit_game(p_game_id); $$;
create or replace function public.contest_statistics() returns jsonb language sql security invoker set search_path='' as $$ select contest_internal.statistics(); $$;

revoke all on all functions in schema contest_internal from public,anon,authenticated;
grant execute on function contest_internal.ensure_profile(),contest_internal.create_game(text,text,timestamptz,timestamptz,integer,text),
  contest_internal.update_profile(text,text,text),
  contest_internal.join_game(uuid),contest_internal.get_game(uuid),contest_internal.start_game(uuid),
  contest_internal.save_result(uuid,integer[]),contest_internal.forfeit_game(uuid),contest_internal.statistics() to authenticated;
revoke all on function public.contest_ensure_profile(),public.contest_create_game(text,text,timestamptz,timestamptz,integer,text),
  public.contest_update_profile(text,text,text),
  public.contest_join_game(uuid),public.contest_get_game(uuid),public.contest_start_game(uuid),
  public.contest_save_result(uuid,integer[]),public.contest_forfeit_game(uuid),public.contest_statistics() from public,anon,authenticated;
grant execute on function public.contest_ensure_profile(),public.contest_create_game(text,text,timestamptz,timestamptz,integer,text),
  public.contest_update_profile(text,text,text),
  public.contest_join_game(uuid),public.contest_get_game(uuid),public.contest_start_game(uuid),
  public.contest_save_result(uuid,integer[]),public.contest_forfeit_game(uuid),public.contest_statistics() to authenticated;

insert into storage.buckets(id,name,public,file_size_limit,allowed_mime_types)
  values('contest-avatars','contest-avatars',true,5242880,array['image/jpeg','image/png']) on conflict(id) do nothing;
drop policy if exists contest_avatar_read on storage.objects;
create policy contest_avatar_read on storage.objects for select to authenticated
  using(bucket_id='contest-avatars');
drop policy if exists contest_avatar_insert on storage.objects;
create policy contest_avatar_insert on storage.objects for insert to authenticated
  with check(bucket_id='contest-avatars' and (storage.foldername(name))[1]=(select auth.uid())::text);
drop policy if exists contest_avatar_update on storage.objects;
create policy contest_avatar_update on storage.objects for update to authenticated
  using(bucket_id='contest-avatars' and (storage.foldername(name))[1]=(select auth.uid())::text)
  with check(bucket_id='contest-avatars' and (storage.foldername(name))[1]=(select auth.uid())::text);

notify pgrst, 'reload schema';
commit;
