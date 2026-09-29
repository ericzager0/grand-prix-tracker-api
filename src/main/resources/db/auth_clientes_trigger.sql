-- Crea la fila en `clientes` cuando un usuario se registra en Supabase Auth.
-- clientes.id_cliente = auth.users.id, que es el claim "sub" del JWT que valida el backend.
-- Ya aplicado en Supabase; se guarda solo como registro.

create or replace function public.handle_new_auth_user()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  insert into public.clientes (id_cliente, nombre, apellido, email)
  values (new.id,
    coalesce(nullif(new.raw_user_meta_data->>'nombre', ''), split_part(new.email, '@', 1)),
    coalesce(new.raw_user_meta_data->>'apellido', ''),
    new.email)
  on conflict (id_cliente) do nothing;
  return new;
end; $$;

create trigger on_auth_user_created after insert on auth.users
  for each row execute function public.handle_new_auth_user();
