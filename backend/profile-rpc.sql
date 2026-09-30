-- Additive profile update API, compatible with Android and JDK HttpURLConnection.
begin;
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
create or replace function public.contest_update_profile(p_name text,p_location text,p_avatar_url text default null)
returns jsonb language sql security invoker set search_path='' as $$ select contest_internal.update_profile(p_name,p_location,p_avatar_url); $$;
revoke all on function contest_internal.update_profile(text,text,text),public.contest_update_profile(text,text,text) from public,anon,authenticated;
grant execute on function contest_internal.update_profile(text,text,text),public.contest_update_profile(text,text,text) to authenticated;
notify pgrst,'reload schema';
commit;
