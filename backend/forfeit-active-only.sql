-- Forfeiting a scheduled match must not bypass the server's start-time validation.
begin;
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
revoke all on function contest_internal.forfeit_game(uuid) from public,anon;
grant execute on function contest_internal.forfeit_game(uuid) to authenticated;
notify pgrst,'reload schema';
commit;
