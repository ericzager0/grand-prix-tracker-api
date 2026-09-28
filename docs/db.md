# Esquema de la base de datos

Base de datos PostgreSQL alojada en Supabase. Todas las tablas usan `uuid` como clave primaria (generado con `extensions.uuid_generate_v4()`, salvo `notificaciones` que usa `gen_random_uuid()`), y la mayoría tiene `created_at timestamptz DEFAULT timezone('utc'::text, now())`.

RLS (Row Level Security) está **habilitado en todas las tablas y no hay políticas definidas**: con la clave pública de Supabase no se puede leer ni escribir nada. El backend no se ve afectado porque se conecta con el usuario `postgres`, que ignora RLS. Si en algún momento el frontend consulta Supabase directamente, hay que crear políticas.

No hay triggers ni funciones propias en el esquema `public`.

## Diagrama de dominios

- **Geografía**: `paises` → `ciudades` → `circuitos` / `hoteles` / `vuelos` (origen y destino)
- **Evento**: `circuitos` → `eventos_f1` → `entradas_gradas`
- **Alojamiento**: `hoteles` → `habitaciones_hotel`
- **Cliente**: `clientes` → `metodos_pago`
- **Reserva**: `reservas` (cabecera) + tablas de detalle por tipo de producto (`reserva_detalle_entradas`, `reserva_detalle_hoteles`, `reserva_detalle_vuelos`)
- **Notificaciones**: `notificaciones`, sin relaciones declaradas con otras tablas

---

## Geografía

### `paises`

| Columna | Tipo | Detalle |
|---|---|---|
| `id_pais` | uuid | PK, default `uuid_generate_v4()` |
| `nombre` | varchar | |
| `codigo_iso` | varchar | UNIQUE. Los datos cargados usan ISO 3166-1 alfa-2 (`AR`, `BR`, `US`…) |
| `continente` | varchar | CHECK: `north-america`, `europe`, `asia`, `middle-east`, `latin-america` |
| `created_at` | timestamptz | default `now()` UTC, nullable |

Referenciada por: `ciudades.id_pais`

### `ciudades`

| Columna | Tipo | Detalle |
|---|---|---|
| `id_ciudad` | uuid | PK |
| `id_pais` | uuid | FK → `paises.id_pais` |
| `nombre` | varchar | |
| `created_at` | timestamptz | |

Referenciada por: `circuitos.id_ciudad`, `hoteles.id_ciudad`, `vuelos.origen_id_ciudad`, `vuelos.destino_id_ciudad`

---

## Circuitos y eventos

### `circuitos`

| Columna | Tipo | Detalle |
|---|---|---|
| `id_circuito` | uuid | PK |
| `id_ciudad` | uuid | FK → `ciudades.id_ciudad` |
| `nombre` | varchar | |
| `longitud_km` | numeric | nullable |
| `curvas` | integer | nullable |
| `vueltas` | integer | nullable |
| `mapa_svg_url` | text | nullable |
| `created_at` | timestamptz | |

Referenciada por: `eventos_f1.id_circuito`

### `eventos_f1`

Un evento es un Gran Premio en una temporada/circuito determinado.

| Columna | Tipo | Detalle |
|---|---|---|
| `id_evento` | uuid | PK |
| `id_circuito` | uuid | FK → `circuitos.id_circuito` |
| `temporada` | integer | |
| `fecha_inicio` | date | |
| `fecha_fin` | date | |
| `estado` | varchar | CHECK: `Proximo`, `En curso`, `Finalizado`. Default `'Proximo'` |
| `created_at` | timestamptz | |

Referenciada por: `entradas_gradas.id_evento`

### `entradas_gradas`

Tipos de entrada (tribuna) disponibles para un evento, con stock.

| Columna | Tipo | Detalle |
|---|---|---|
| `id_entrada` | uuid | PK |
| `id_evento` | uuid | FK → `eventos_f1.id_evento` |
| `nombre_tribuna` | varchar | |
| `precio_usd` | numeric | |
| `stock_disponible` | integer | default `0` |
| `tipo` | varchar | CHECK: `VIP`, `Asiento Numerado`, `General` |
| `created_at` | timestamptz | |

Referenciada por: `reserva_detalle_entradas.id_entrada`

---

## Alojamiento

### `hoteles`

| Columna | Tipo | Detalle |
|---|---|---|
| `id_hotel` | uuid | PK |
| `id_ciudad` | uuid | FK → `ciudades.id_ciudad` |
| `nombre` | varchar | |
| `estrellas` | integer | CHECK: entre 1 y 5, nullable |
| `distancia_circuito_km` | numeric | nullable |
| `ofrece_traslado` | boolean | default `false` |
| `imagen_principal_url` | text | nullable |
| `created_at` | timestamptz | |

Referenciada por: `habitaciones_hotel.id_hotel`

### `habitaciones_hotel`

Tipos de habitación disponibles por hotel, con stock.

| Columna | Tipo | Detalle |
|---|---|---|
| `id_habitacion` | uuid | PK |
| `id_hotel` | uuid | FK → `hoteles.id_hotel` |
| `tipo` | varchar | CHECK: `Single`, `Doble`, `Suite` |
| `precio_por_noche_usd` | numeric | |
| `stock_disponible` | integer | default `0` |
| `created_at` | timestamptz | |

Referenciada por: `reserva_detalle_hoteles.id_habitacion`

---

## Vuelos

### `vuelos`

| Columna | Tipo | Detalle |
|---|---|---|
| `id_vuelo` | uuid | PK |
| `aerolinea` | varchar | |
| `origen_id_ciudad` | uuid | FK → `ciudades.id_ciudad` |
| `destino_id_ciudad` | uuid | FK → `ciudades.id_ciudad` |
| `fecha_salida` | timestamptz | |
| `fecha_llegada` | timestamptz | |
| `precio_usd` | numeric | |
| `stock_asientos` | integer | default `0` |
| `created_at` | timestamptz | |

Referenciada por: `reserva_detalle_vuelos.id_vuelo`

---

## Clientes y pagos

### `clientes`

| Columna | Tipo | Detalle |
|---|---|---|
| `id_cliente` | uuid | PK |
| `nombre` | varchar | |
| `apellido` | varchar | |
| `email` | varchar | UNIQUE |
| `telefono` | varchar | nullable |
| `created_at` | timestamptz | |

Referenciada por: `metodos_pago.id_cliente`, `reservas.id_cliente`

> Cuando se integre Supabase Auth, `id_cliente` va a coincidir con el id del usuario autenticado (claim `sub` del JWT).

### `metodos_pago`

| Columna | Tipo | Detalle |
|---|---|---|
| `id_metodo` | uuid | PK |
| `id_cliente` | uuid | FK → `clientes.id_cliente` |
| `tipo` | varchar | CHECK: `Credito`, `Debito` |
| `ultimos_4_digitos` | varchar | |
| `proveedor_token` | text | token de la pasarela de pago — no se almacena el número completo de la tarjeta |
| `fecha_expiracion` | varchar | |
| `created_at` | timestamptz | |

Referenciada por: `reservas.id_metodo_pago`

---

## Reservas

### `reservas`

Cabecera de una reserva. Los flags `incluye_*` indican qué tipos de producto componen la reserva; el detalle real vive en las tablas `reserva_detalle_*`.

| Columna | Tipo | Detalle |
|---|---|---|
| `id_reserva` | uuid | PK |
| `id_cliente` | uuid | FK → `clientes.id_cliente` |
| `id_metodo_pago` | uuid | FK → `metodos_pago.id_metodo`, nullable |
| `codigo_confirmacion` | varchar | UNIQUE |
| `total_usd` | numeric | |
| `estado` | varchar | CHECK: `Pendiente`, `Pagada`, `Cancelada`. Default `'Pendiente'` |
| `fecha_compra` | timestamptz | default `now()` UTC |
| `incluye_hotel` | boolean | default `false` |
| `incluye_entrada` | boolean | default `false` |
| `incluye_vuelo` | boolean | default `false` |
| `incluye_transporte` | boolean | default `false` |

Referenciada por: `reserva_detalle_entradas.id_reserva`, `reserva_detalle_hoteles.id_reserva`, `reserva_detalle_vuelos.id_reserva`

> Nota: existe el flag `incluye_transporte` pero no hay una tabla `reserva_detalle_transporte` ni tabla de transporte en el esquema actual — a confirmar si es funcionalidad pendiente de implementar.

### `reserva_detalle_entradas`

| Columna | Tipo | Detalle |
|---|---|---|
| `id_detalle_entrada` | uuid | PK |
| `id_reserva` | uuid | FK → `reservas.id_reserva` |
| `id_entrada` | uuid | FK → `entradas_gradas.id_entrada` |
| `cantidad` | integer | CHECK: `> 0` |
| `subtotal_usd` | numeric | |

### `reserva_detalle_hoteles`

| Columna | Tipo | Detalle |
|---|---|---|
| `id_detalle_hotel` | uuid | PK |
| `id_reserva` | uuid | FK → `reservas.id_reserva` |
| `id_habitacion` | uuid | FK → `habitaciones_hotel.id_habitacion` |
| `fecha_check_in` | date | |
| `fecha_check_out` | date | |
| `cantidad_noches` | integer | CHECK: `> 0` |
| `subtotal_usd` | numeric | |

### `reserva_detalle_vuelos`

| Columna | Tipo | Detalle |
|---|---|---|
| `id_detalle_vuelo` | uuid | PK |
| `id_reserva` | uuid | FK → `reservas.id_reserva` |
| `id_vuelo` | uuid | FK → `vuelos.id_vuelo` |
| `cantidad_pasajeros` | integer | CHECK: `> 0` |
| `subtotal_usd` | numeric | |

---

## Notificaciones

### `notificaciones`

Tabla creada por otro integrante del equipo. El backend todavía no la usa (no tiene entidad ni endpoints).

| Columna | Tipo | Detalle |
|---|---|---|
| `id_notificacion` | uuid | PK, default `gen_random_uuid()` |
| `id_usuario` | uuid | nullable. Sin FK declarada |
| `titulo` | varchar | |
| `mensaje` | text | |
| `tipo` | varchar | Sin CHECK. Los datos actuales usan `OFFER` |
| `leido` | boolean | default `false` |
| `url_destino` | varchar | nullable |
| `metadata` | text | nullable |
| `creado_en` | timestamptz | default `now()`, nullable. Ojo: se llama `creado_en`, no `created_at` como en el resto de las tablas |

Índices: `idx_notificaciones_id_usuario (id_usuario)`, `idx_notificaciones_usuario_leido (id_usuario, leido)`, `idx_notificaciones_creado_en (creado_en DESC)`.

---

## Índices

Además de los índices de PK y UNIQUE que crea Postgres, existen estos (script en [`src/main/resources/db/indexes.sql`](../src/main/resources/db/indexes.sql), ya aplicado):

| Tabla | Índice | Columnas |
|---|---|---|
| `paises` | `idx_paises_continente` | `continente` |
| `ciudades` | `idx_ciudades_id_pais` | `id_pais` |
| `ciudades` | `idx_ciudades_nombre` | `nombre` |
| `circuitos` | `idx_circuitos_id_ciudad` | `id_ciudad` |
| `circuitos` | `idx_circuitos_nombre` | `nombre` |
| `eventos_f1` | `idx_eventos_f1_temporada` | `temporada` |
| `eventos_f1` | `idx_eventos_f1_fecha_inicio` | `fecha_inicio` |
| `eventos_f1` | `idx_eventos_f1_id_circuito` | `id_circuito` |
| `eventos_f1` | `idx_eventos_f1_temp_fecha` | `temporada, fecha_inicio` |
| `eventos_f1` | `idx_eventos_f1_estado` | `estado` |
| `notificaciones` | ver arriba | |

Las FKs de entradas, hoteles, habitaciones, vuelos y reservas no tienen índice propio. Con el volumen actual no hace falta; si las tablas crecen, conviene indexar `entradas_gradas.id_evento`, `hoteles.id_ciudad`, `habitaciones_hotel.id_hotel` y `vuelos.origen_id_ciudad` / `destino_id_ciudad`, que son las columnas por las que filtran los listados del wizard.

---

## Datos de prueba

[`src/main/resources/db/seed.sql`](../src/main/resources/db/seed.sql) carga entradas, hoteles, habitaciones, vuelos, clientes y tarjetas sobre los eventos existentes. El detalle de qué id es cada cosa está en [`api.md`](./api.md#datos-de-prueba).

La lógica de negocio detrás de `reservas.estado` y del descuento de stock está en [`checkout.md`](./checkout.md).
