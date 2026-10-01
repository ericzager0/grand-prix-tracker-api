-- ============================================================================
-- Sincronización entre Supabase Auth y la tabla `clientes` + Políticas RLS
-- clientes.id_cliente = auth.users.id (claim "sub" del JWT de Supabase).
-- ============================================================================

-- 1. TRIGGER: auth.users -> public.clientes
-- Cuando un usuario se registra o actualiza en Supabase Auth, se sincroniza en `clientes`
-- incluyendo nombre, apellido, email, teléfono y DNI.
create or replace function public.handle_new_auth_user()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  insert into public.clientes (id_cliente, nombre, apellido, email, telefono, dni)
  values (
    new.id,
    coalesce(nullif(new.raw_user_meta_data->>'nombre', ''), split_part(new.email, '@', 1)),
    coalesce(new.raw_user_meta_data->>'apellido', ''),
    new.email,
    coalesce(nullif(new.raw_user_meta_data->>'telefono', ''), new.phone),
    nullif(new.raw_user_meta_data->>'dni', '')::numeric
  )
  on conflict (id_cliente) do update
  set
    nombre = coalesce(nullif(excluded.nombre, ''), clientes.nombre),
    apellido = coalesce(nullif(excluded.apellido, ''), clientes.apellido),
    email = coalesce(excluded.email, clientes.email),
    telefono = coalesce(excluded.telefono, clientes.telefono),
    dni = coalesce(excluded.dni, clientes.dni);
  return new;
end; $$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert or update on auth.users
  for each row execute function public.handle_new_auth_user();

-- 2. TRIGGER: public.clientes -> auth.users (Opción A: sincroniza user_metadata)
-- Cuando se inserte o actualice `telefono` o `dni` en `clientes`, se actualiza automáticamente
-- en `auth.users.raw_user_meta_data` para que el frontend lo lea de inmediato
-- sin necesidad de queries directas a la base de datos.
create or replace function public.sync_cliente_to_auth_user()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  if (new.telefono is not null and (old is null or new.telefono is distinct from old.telefono)) or
     (new.dni is not null and (old is null or new.dni is distinct from old.dni)) then
    update auth.users
    set raw_user_meta_data = coalesce(raw_user_meta_data, '{}'::jsonb) ||
      jsonb_strip_nulls(jsonb_build_object(
        'telefono', new.telefono,
        'dni', new.dni
      ))
    where id = new.id_cliente;
  end if;
  return new;
end; $$;

drop trigger if exists on_cliente_updated on public.clientes;
create trigger on_cliente_updated
  after insert or update of telefono, dni on public.clientes
  for each row execute function public.sync_cliente_to_auth_user();

-- 3. BACKFILL: Sincronizar teléfonos y DNI existentes hacia auth.users
-- Copia los teléfonos y DNIs que ya existan en la tabla `clientes` hacia `auth.users.raw_user_meta_data`.
update auth.users u
set raw_user_meta_data = coalesce(u.raw_user_meta_data, '{}'::jsonb) ||
  jsonb_strip_nulls(jsonb_build_object(
    'telefono', c.telefono,
    'dni', c.dni
  ))
from public.clientes c
where u.id = c.id_cliente
  and (c.telefono is not null or c.dni is not null);

-- 4. POLÍTICAS RLS: Habilitar lectura y edición directa en clientes (Opción B)
-- Permite que el front haga: supabase.from('clientes').select('telefono').eq('id_cliente', user.id).single()
alter table public.clientes enable row level security;

drop policy if exists "Clientes pueden leer su propio perfil" on public.clientes;
create policy "Clientes pueden leer su propio perfil"
  on public.clientes
  for select
  to authenticated
  using (id_cliente = (select auth.uid()));

drop policy if exists "Clientes pueden actualizar su propio perfil" on public.clientes;
create policy "Clientes pueden actualizar su propio perfil"
  on public.clientes
  for update
  to authenticated
  using (id_cliente = (select auth.uid()))
  with check (id_cliente = (select auth.uid()));
