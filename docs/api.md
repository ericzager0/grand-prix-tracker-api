# API — Referencia de endpoints

Contrato HTTP del backend para quien consuma la API, principalmente el frontend `grand-prix-tracker-web`. Acá está todo lo necesario para integrar el flujo de compra: qué recibe, qué devuelve y qué hace cada endpoint, cómo encadenarlos en el wizard y con qué datos probar.

- Reglas de negocio y decisiones detrás del checkout: [`checkout.md`](./checkout.md)
- Modelo de datos: [`db.md`](./db.md)

## Índice

| Método | Ruta | Para qué |
|---|---|---|
| `GET` | [`/events`](#get-events) | Calendario de Grandes Premios |
| `GET` | [`/events/{id}`](#get-eventsid) | Detalle de un Gran Premio |
| `GET` | [`/events/{id}/tickets`](#get-eventsidtickets) | Entradas disponibles del evento |
| `GET` | [`/events/{id}/hotels`](#get-eventsidhotels) | Hoteles y habitaciones en la ciudad del evento |
| `GET` | [`/events/{id}/flights`](#get-eventsidflights) | Vuelos de ida y vuelta a la ciudad del evento |
| `GET` | [`/payment-methods`](#get-payment-methods) | Tarjetas guardadas del cliente |
| `POST` | [`/bookings`](#post-bookings) | Comprar el paquete |

---

## Generalidades

- **Base URL**: `https://grand-prix-tracker-api.onrender.com` en producción, `http://localhost:8080` en local. En el front se configura con `NEXT_PUBLIC_API_URL`. El plan free de Render duerme la instancia: la primera request después de un rato de inactividad puede tardar bastante, así que conviene reintentar (el front ya lo hace con TanStack Query).
- **Formato**: JSON. Campos en camelCase y en español.
- **Fechas**: sin hora como `"YYYY-MM-DD"`; con hora como ISO-8601 (`"2026-11-05T12:00:00Z"`).
- **Montos**: números decimales en USD (`350.00`).
- **IDs**: UUID en string.
- **CORS**: habilitado para cualquier origen.

### Identificación del cliente (temporal)

Todavía no hay autenticación. Los endpoints que dependen del cliente (`GET /payment-methods` y `POST /bookings`) reciben su id en el header **`X-Cliente-Id`**. Para desarrollo, usar el cliente de prueba `99999999-0000-4000-8000-000000000001`.

Cuando se integre Supabase Auth, ese header se reemplaza por `Authorization: Bearer <jwt>` y el backend toma el cliente del claim `sub`. El resto del contrato no cambia.

### Envoltorio de respuesta

Todas las respuestas, exitosas o no, usan el mismo envoltorio (ya tipado en el front como `ApiResponse<T>`):

```ts
interface ApiResponse<T> {
  success: boolean;
  message: string;   // texto legible; en errores se puede mostrar al usuario
  data: T | null;    // null cuando success = false
}
```

### Códigos de error

| Código | Significado |
|---|---|
| `400` | Request inválida: body mal formado, validación de campos, header faltante, UUID inválido o una regla de negocio incumplida. |
| `404` | El recurso pedido, o alguno referenciado en el body, no existe. |
| `409` | Conflicto de stock: alguien compró antes y ya no alcanza. |
| `500` | Error inesperado del servidor. |

---

## Flujo del wizard de compra

El wizard vive en `/booking/[id]/...` y arma el paquete en memoria. Solo al confirmar le pega al backend para comprar.

| Paso | Endpoint | Qué guardar en memoria |
|---|---|---|
| 0. Evento elegido | `GET /events/{id}` | `idEvento`, fechas del evento, ciudad |
| 1. Hotel | `GET /events/{id}/hotels` | por cada habitación elegida: `idHabitacion`, `fechaCheckIn`, `fechaCheckOut` |
| 2. Entradas | `GET /events/{id}/tickets` | `idEntrada` y `cantidad` |
| 3. Vuelos | `GET /events/{id}/flights` | `idVuelo` y `cantidadPasajeros` (una ida y una vuelta, filtrando por `sentido`) |
| 4. Pago | `GET /payment-methods` | `idMetodoPago` de una tarjeta guardada, o los datos de una tarjeta nueva |
| 5. Confirmar | `POST /bookings` | La respuesta trae `codigoConfirmacion`, el total y el detalle para la pantalla de confirmación |

Recomendaciones:

- **Todos los productos son opcionales**, pero el paquete necesita al menos uno. El wizard puede permitir saltear pasos.
- **El precio que muestra el front es orientativo**: el total real lo calcula el backend y viene en la respuesta del `POST`. Para el resumen previo se puede calcular igual: `precioUsd × cantidad` para entradas, `precioPorNocheUsd × noches` para hotel y `precioUsd × cantidadPasajeros` para vuelos.
- **Mostrar el stock y deshabilitar lo agotado** (`stockDisponible === 0` / `stockAsientos === 0`). Aun así, entre que el usuario elige y confirma alguien puede comprar lo último: en ese caso el `POST` responde `409` y el `message` dice qué ítem se agotó. Lo ideal es volver al paso de ese ítem sin perder el resto del paquete.
- **No confiar en `estado` del evento para saber si ya pasó.** Hay eventos cargados como `Proximo` cuyas fechas ya pasaron. Usar `fechaFin`: el backend rechaza la compra si `fechaFin` es anterior a hoy.
- **Tarjetas vencidas**: `GET /payment-methods` las marca con `vencida: true`. El `POST` las rechaza, así que conviene deshabilitarlas.

---

## Eventos

### `GET /events`

Lista los Grandes Premios con circuito, ciudad y país, ordenados por fecha de inicio ascendente.

**Query params**

| Param | Tipo | Requerido | Descripción |
|---|---|---|---|
| `temporada` | integer | no | Filtra por año de temporada (ej. `2026`). Sin este param devuelve todos. |

**Respuesta `200`**: `ApiResponse<Evento[]>`

```ts
interface Evento {
  idEvento: string;
  temporada: number;
  fechaInicio: string;      // "YYYY-MM-DD"
  fechaFin: string;         // "YYYY-MM-DD"
  estado: "Proximo" | "En curso" | "Finalizado";  // puede estar desactualizado, ver "Flujo del wizard"
  circuito: {
    idCircuito: string;
    nombre: string;
    longitudKm: number | null;
    curvas: number | null;
    vueltas: number | null;
    mapaSvgUrl: string | null;
    ciudad: {
      idCiudad: string;
      nombre: string;
      pais: {
        idPais: string;
        nombre: string;
        codigoIso: string;  // ISO 3166-1 alfa-2, ej. "BR"
        continente: "north-america" | "europe" | "asia" | "middle-east" | "latin-america";
      };
    };
  };
}
```

**Errores**: `400` si `temporada` no es un número.

### `GET /events/{id}`

Un evento puntual, con la misma forma que cada ítem de `GET /events`.

**Respuesta `200`**: `ApiResponse<Evento>`

**Errores**: `400` si `id` no es un UUID · `404` si el evento no existe.

### `GET /events/{id}/tickets`

Entradas (tribunas) a la venta para el evento, ordenadas por precio ascendente. Incluye las agotadas, con `stockDisponible: 0`.

**Respuesta `200`**: `ApiResponse<Entrada[]>`

```ts
interface Entrada {
  idEntrada: string;
  nombreTribuna: string;
  tipo: "VIP" | "Asiento Numerado" | "General";
  precioUsd: number;        // por entrada
  stockDisponible: number;
}
```

**Errores**: `400` si `id` no es un UUID · `404` si el evento no existe.

### `GET /events/{id}/hotels`

Hoteles en la ciudad del circuito del evento, cada uno con sus tipos de habitación. Ordenados por distancia al circuito (más cercano primero); dentro de cada hotel, las habitaciones van de la más barata a la más cara. Solo aparecen hoteles que tienen al menos un tipo de habitación, e incluye las agotadas con `stockDisponible: 0`.

**Respuesta `200`**: `ApiResponse<Hotel[]>`

```ts
interface Hotel {
  idHotel: string;
  nombre: string;
  estrellas: number | null;             // 1 a 5
  distanciaCircuitoKm: number | null;
  ofreceTraslado: boolean | null;
  imagenPrincipalUrl: string | null;
  habitaciones: {
    idHabitacion: string;
    tipo: "Single" | "Doble" | "Suite";
    precioPorNocheUsd: number;
    stockDisponible: number;            // habitaciones de este tipo disponibles, sin distinguir fechas
  }[];
}
```

Para comprar, cada habitación se manda en el `POST /bookings` con sus fechas de check-in y check-out. El stock no depende de las fechas: ver limitaciones en [`checkout.md`](./checkout.md#limitaciones-conocidas).

**Errores**: `400` si `id` no es un UUID · `404` si el evento no existe.

### `GET /events/{id}/flights`

Vuelos que llegan a la ciudad del evento (**ida**) o salen de ella (**vuelta**), solo los que todavía no partieron. Ordenados por fecha de salida.

**Respuesta `200`**: `ApiResponse<Vuelo[]>`

```ts
interface Vuelo {
  idVuelo: string;
  aerolinea: string;
  sentido: "IDA" | "VUELTA";   // IDA = llega a la ciudad del evento; VUELTA = sale de ella
  origen: { idCiudad: string; nombre: string };
  destino: { idCiudad: string; nombre: string };
  fechaSalida: string;         // ISO-8601
  fechaLlegada: string;        // ISO-8601
  precioUsd: number;           // por pasajero
  stockAsientos: number;
}
```

**Errores**: `400` si `id` no es un UUID · `404` si el evento no existe.

---

## Pagos

### `GET /payment-methods`

Tarjetas guardadas del cliente, de la más nueva a la más vieja. Nunca expone el token de la pasarela.

**Headers**: `X-Cliente-Id` (ver [Identificación del cliente](#identificación-del-cliente-temporal)).

**Respuesta `200`**: `ApiResponse<MetodoPago[]>`

```ts
interface MetodoPago {
  idMetodoPago: string;
  tipo: "Credito" | "Debito";
  ultimos4Digitos: string;
  fechaExpiracion: string;   // "MM/AA"
  vencida: boolean;          // true si ya venció; el checkout la rechaza
}
```

El tipo `Credito`/`Debito` no indica la marca (Visa, Mastercard); hoy la base no guarda la marca.

**Errores**: `400` si falta `X-Cliente-Id` o no es un UUID · `404` si el cliente no existe.

---

## Reservas

### `POST /bookings`

Compra un paquete para un evento: entradas, habitaciones de hotel y vuelos, pagando en la misma llamada. Es la llamada final del wizard.

Qué hace, en orden:
1. Valida el paquete: al menos un ítem, evento no finalizado, fechas de hotel coherentes.
2. Valida el medio de pago: tarjeta guardada del cliente y no vencida, o tarjeta nueva no vencida, que queda guardada.
3. Por cada ítem: verifica que corresponda al evento o a su ciudad y descuenta el stock.
4. Calcula precios y total con los valores de la base y crea la reserva en estado `Pagada`.

Si cualquier paso falla no se aplica nada: ni stock, ni tarjeta, ni reserva.

**Headers**

| Header | Requerido | Descripción |
|---|---|---|
| `X-Cliente-Id` | sí | UUID del cliente que compra (ver [Identificación del cliente](#identificación-del-cliente-temporal)). |
| `Content-Type` | sí | `application/json` |

**Body**

```ts
interface CheckoutRequest {
  idEvento: string;
  entradas?: { idEntrada: string; cantidad: number }[];                                    // cantidad >= 1
  habitaciones?: { idHabitacion: string; fechaCheckIn: string; fechaCheckOut: string }[]; // "YYYY-MM-DD"
  vuelos?: { idVuelo: string; cantidadPasajeros: number }[];                               // cantidadPasajeros >= 1
  pago: PagoConTarjetaGuardada | PagoConTarjetaNueva;
}

interface PagoConTarjetaGuardada {
  idMetodoPago: string;     // de GET /payment-methods; tiene que ser del cliente y no estar vencida
}

interface PagoConTarjetaNueva {
  tipo: "Credito" | "Debito";
  ultimos4Digitos: string;  // exactamente 4 dígitos
  fechaExpiracion: string;  // "MM/AA", no vencida
  proveedorToken: string;   // token de la pasarela; hoy es simulado, cualquier string no vacío
}
```

Reglas del body:
- `entradas`, `habitaciones` y `vuelos` son opcionales, pero tiene que haber **al menos un ítem en total**.
- Cada ítem de `habitaciones` es **una** habitación; para dos habitaciones se mandan dos ítems.
- Las entradas tienen que ser del evento. Los hoteles tienen que estar en la ciudad del circuito. Los vuelos tienen que llegar a o salir de esa ciudad y no haber partido.
- `fechaCheckOut` tiene que ser posterior a `fechaCheckIn`, y `fechaCheckIn` no puede ser anterior a hoy.
- **No se mandan precios ni totales**: los calcula el backend.
- Con una tarjeta nueva, **el número completo nunca viaja al backend**: solo los últimos 4 dígitos y el token de la pasarela.

Ejemplo (São Paulo, 2 personas, 4 noches, tarjeta guardada):

```json
{
  "idEvento": "c25c1812-9ced-4f15-ad77-842e52214f05",
  "entradas": [{ "idEntrada": "55555555-0000-4000-8000-000000000017", "cantidad": 2 }],
  "habitaciones": [{ "idHabitacion": "77777777-0000-4000-8000-000000000021", "fechaCheckIn": "2026-11-05", "fechaCheckOut": "2026-11-09" }],
  "vuelos": [
    { "idVuelo": "88888888-0000-4000-8000-000000000007", "cantidadPasajeros": 2 },
    { "idVuelo": "88888888-0000-4000-8000-000000000009", "cantidadPasajeros": 2 }
  ],
  "pago": { "idMetodoPago": "aaaaaaaa-0000-4000-8000-000000000001" }
}
```

**Respuesta `201`**: `ApiResponse<Reserva>`, con `message: "Reserva confirmada"`

```ts
interface Reserva {
  idReserva: string;
  codigoConfirmacion: string;  // "GP-XXXXXXXX", para mostrarle al usuario
  estado: "Pagada";
  fechaCompra: string;         // ISO-8601
  totalUsd: number;
  incluyeEntrada: boolean;
  incluyeHotel: boolean;
  incluyeVuelo: boolean;
  idMetodoPago: string;        // si se pagó con tarjeta nueva, es el id con el que quedó guardada
  entradas: {
    idEntrada: string;
    nombreTribuna: string;
    tipo: "VIP" | "Asiento Numerado" | "General";
    cantidad: number;
    precioUnitarioUsd: number;
    subtotalUsd: number;
  }[];
  habitaciones: {
    idHabitacion: string;
    hotel: string;             // nombre del hotel
    tipo: "Single" | "Doble" | "Suite";
    fechaCheckIn: string;
    fechaCheckOut: string;
    cantidadNoches: number;
    precioPorNocheUsd: number;
    subtotalUsd: number;
  }[];
  vuelos: {
    idVuelo: string;
    aerolinea: string;
    origen: string;            // nombre de la ciudad
    destino: string;           // nombre de la ciudad
    fechaSalida: string;
    fechaLlegada: string;
    cantidadPasajeros: number;
    precioUnitarioUsd: number;
    subtotalUsd: number;
  }[];
}
```

Para el ejemplo de arriba: 2 × 520 (entradas) + 4 noches × 190 (hotel) + 2 × 320 + 2 × 310 (vuelos) = `totalUsd: 3060.00`.

**Errores**

| Código | Casos |
|---|---|
| `400` | Falta `X-Cliente-Id` o no es UUID · body mal formado · campo inválido (el `message` lista los campos, ej. `"Datos inválidos: entradas[0].cantidad: must be greater than 0"`) · paquete vacío · evento finalizado o con `fechaFin` pasada · entrada de otro evento · hotel o vuelo fuera de la ciudad del evento · vuelo ya partido · fechas de hotel inválidas · tarjeta vencida o datos de tarjeta incompletos. |
| `404` | No existe el cliente, el evento, alguna entrada, habitación o vuelo, o el `idMetodoPago` no es del cliente. |
| `409` | Sin stock suficiente. El `message` indica cuál, ej. `"No hay stock suficiente para la tribuna Paddock Club"`. |

---

## Endpoints que todavía no existen

- **Reservas del cliente** (`ReservasView` del perfil) y **detalle de una reserva**. La pantalla de confirmación no los necesita: `POST /bookings` ya devuelve todo.
- **Alta y baja de tarjetas** (`AddPaymentModal` del perfil). Hoy una tarjeta nueva solo se guarda al usarla en una compra.
- **Datos del cliente** (`DatosView` del perfil) y registro. Llegan con Supabase Auth.
- **Traslados**: no existen en la base (ver [`checkout.md`](./checkout.md#limitaciones-conocidas)).

---

## Datos de prueba

Los países, ciudades, circuitos y eventos ya estaban cargados en Supabase. [`src/main/resources/db/seed.sql`](../src/main/resources/db/seed.sql) agrega el resto con ids fijos: entradas, hoteles, habitaciones, vuelos, clientes, tarjetas y Buenos Aires como origen de los vuelos.

**Clientes** (para `X-Cliente-Id`)

| id | Cliente | Tarjetas guardadas (`idMetodoPago`) |
|---|---|---|
| `99999999-0000-4000-8000-000000000001` | Cliente Prueba | `aaaaaaaa-0000-4000-8000-000000000001` (Crédito 4242), `aaaaaaaa-0000-4000-8000-000000000002` (Débito 8812) |
| `99999999-0000-4000-8000-000000000002` | Ana Pilotti | `aaaaaaaa-0000-4000-8000-000000000003` (Crédito 1111) |

**Eventos 2026 y sus productos**

Los productos usan UUIDs con prefijo por tabla y el número al final: entrada `017` = `55555555-0000-4000-8000-000000000017`, habitación `021` = `77777777-0000-4000-8000-000000000021`, vuelo `007` = `88888888-0000-4000-8000-000000000007`.

| GP | `idEvento` | Fechas | Entradas | Hoteles → habitaciones | Vuelos ida / vuelta |
|---|---|---|---|---|---|
| Madrid | `992ae124-3d59-4adb-9fd2-f0e825c605e8` | 11–13 sep (ya pasó) | 001, 002, 003 | Gran Vía Madrid → 001, 002 · Barajas Express → 003, 004 | — |
| Bakú | `f1a34d7f-3f76-446b-bed9-22fced10166e` | 25–27 sep (ya pasó) | 004, 005, 006 | Caspian Boulevard → 005, 006 · Old City Inn → 007, 008 | — |
| Singapur | `28780217-2728-4321-865d-db4de3d9ec26` | 9–11 oct | 007, 008, 009 | Marina Bay Harbour → 009, 010 · Bugis Budget Stay → 011, 012 | 001 / 002 |
| Austin | `b3ebbdfa-a64b-4e3f-a0a8-782af0b5b472` | 23–25 oct | 010, 011, 012 | Lone Star Suites → 013, 014 · Congress Avenue → 015, 016 | 003 / 004 |
| Ciudad de México | `9f2762ef-6bde-4ee4-8c43-2a701e887162` | 30 oct – 1 nov | 013, 014, 015 | Reforma Central → 017, 018 · Casa Condesa → 019, 020 | 005 / 006 |
| São Paulo | `c25c1812-9ced-4f15-ad77-842e52214f05` | 6–8 nov | 016, 017, 018 | Interlagos Plaza → 021, 022 · Paulista Grand → 023, 024 | 007, 008 / 009 |
| Las Vegas | `0807f206-e3b8-4855-9ea7-2f8b8db1cfb7` | 19–21 nov | 019, 020, 021 | Strip View Resort → 025, 026 · Desert Inn Express → 027, 028 | 010 / 011 |
| Lusail | `4513e753-0b39-4dbd-84a1-01eb594cbf09` | 27–29 nov | 022, 023, 024 | Lusail Marina → 029, 030 · Doha Corniche Suites → 031, 032 | 012 / 013 |
| Abu Dabi | `691efae9-acc9-4c45-95dc-210acf720d83` | 4–6 dic | 025, 026, 027 | Yas Marina Waterfront → 033, 034 · Corniche Bay → 035, 036 | 014 / 015 |

En cada evento las entradas van en orden General, Asiento Numerado y VIP. Todos los vuelos salen de Buenos Aires o vuelven ahí. Madrid y Bakú no tienen vuelos porque sus fechas ya pasaron; siguen figurando como `Proximo` en la base.

**Casos preparados para probar errores**

| Caso | Datos | Resultado esperado |
|---|---|---|
| Entrada agotada | entrada `021` (Paddock Club, Las Vegas) | `409` |
| Habitación agotada | habitación `028` (Doble, Desert Inn Express, Las Vegas) | `409` |
| Último asiento | vuelo `008` (LATAM a São Paulo, 1 asiento): la primera compra de 1 pasajero pasa, la segunda da `409` | `201`, luego `409` |
| Evento ya pasado | evento Madrid con entrada `001` | `400` |
| Vuelo ya partido | vuelo `016` (a São Paulo, 1 sep) con el evento São Paulo. No aparece en `GET /flights` | `400` |
| Entrada de otro evento | entrada `001` (Madrid) con el evento São Paulo | `400` |
| Hotel de otra ciudad | habitación `001` (Madrid) con el evento São Paulo | `400` |
| Tarjeta de otro cliente | cliente `99999999-0000-4000-8000-000000000001` pagando con `idMetodoPago` `aaaaaaaa-0000-4000-8000-000000000003` | `404` |

Las compras de prueba descuentan stock de verdad. Para volver al estado inicial hay que restaurar `stock_disponible` / `stock_asientos` con los valores del seed y borrar las reservas creadas.
