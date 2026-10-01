# API — Referencia de endpoints

Contrato HTTP del backend para quien consuma la API, principalmente el frontend `grand-prix-tracker-web`. Acá está todo lo necesario para integrar el flujo de compra: qué recibe, qué devuelve y qué hace cada endpoint, cómo encadenarlos en el wizard y con qué datos probar.

- Reglas de negocio y decisiones detrás del checkout: [`checkout.md`](./checkout.md)
- Modelo de datos: [`db.md`](./db.md)

## Índice

| Método | Ruta | Para qué | Token |
|---|---|---|---|
| `GET` | [`/events`](#get-events) | Calendario de Grandes Premios | no |
| `GET` | [`/events/{id}`](#get-eventsid) | Detalle de un Gran Premio | no |
| `GET` | [`/events/{id}/tickets`](#get-eventsidtickets) | Entradas disponibles del evento | no |
| `GET` | [`/events/{id}/hotels`](#get-eventsidhotels) | Hoteles y habitaciones en la ciudad del evento | no |
| `GET` | [`/events/{id}/flights`](#get-eventsidflights) | Vuelos de ida y vuelta a la ciudad del evento | no |
| `GET` | [`/payment-methods`](#get-payment-methods) | Tarjetas guardadas del cliente | sí |
| `GET` | [`/users/profile`](#get-usersprofile) | Datos del perfil del cliente | sí |
| `PUT` | [`/users/profile`](#put-usersprofile) | Actualizar perfil del cliente (nombre, apellido, teléfono, DNI) | sí |
| `POST` | [`/bookings`](#post-bookings) | Comprar el paquete | sí |
| `GET` | [`/bookings`](#get-bookings) | Reservas del cliente (perfil) | sí |
| `GET` | [`/bookings/{idReserva}`](#get-bookingsidreserva) | Detalle de una reserva ("Ver itinerario completo") | sí |

---

## Generalidades

- **Base URL**: `https://grand-prix-tracker-api.onrender.com` en producción, `http://localhost:8080` en local. En el front se configura con `NEXT_PUBLIC_API_URL`. El plan free de Render duerme la instancia: la primera request después de un rato de inactividad puede tardar bastante, así que conviene reintentar (el front ya lo hace con TanStack Query).
- **Formato**: JSON. Campos en camelCase y en español.
- **Fechas**: sin hora como `"YYYY-MM-DD"`; con hora como ISO-8601 (`"2026-11-05T12:00:00Z"`).
- **Montos**: números decimales en USD (`350.00`).
- **IDs**: UUID en string.
- **CORS**: habilitado para cualquier origen.

### Identificación del cliente

El usuario inicia sesión en el front con Supabase Auth. Los endpoints que dependen del cliente (`GET /payment-methods`, `POST /bookings`, `GET /bookings` y `GET /bookings/{idReserva}`) exigen el `access_token` de esa sesión:

```
Authorization: Bearer <access_token de Supabase>
```

El backend verifica el token (firma ES256 contra el JWKS del proyecto, emisor `https://zprznayvpeijjoiknird.supabase.co/auth/v1`, audiencia `authenticated` y vencimiento) y toma el cliente del claim `sub`, que es igual a `clientes.id_cliente`: al registrarse, un trigger sobre `auth.users` crea la fila en `clientes` ([`auth_clientes_trigger.sql`](../src/main/resources/db/auth_clientes_trigger.sql)). El header `X-Cliente-Id` ya no se usa y se ignora.

Sin token, o con uno vencido o inválido, esos endpoints responden `401` con el envoltorio de siempre:

```json
{ "success": false, "message": "Se requiere iniciar sesión para acceder a este recurso", "data": null }
```

| Caso | `message` |
|---|---|
| Falta el header `Authorization` | `"Se requiere iniciar sesión para acceder a este recurso"` |
| Token vencido, con firma inválida, de otro proyecto o mal formado | `"La sesión es inválida o expiró"` |

Ante un `401` el front debería refrescar la sesión (o mandar al login) y reintentar.

Los endpoints públicos (`GET /events/**`, incluidos `/tickets`, `/hotels` y `/flights`, y el servicio SOAP `/ws`) no necesitan token. Si llega uno se ignora, así que un token vencido no los rompe.

**Pendiente:** `/notifications/**` todavía identifica al usuario con `?userId=` y no verifica token.

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
| `400` | Request inválida: body mal formado, validación de campos, UUID inválido o una regla de negocio incumplida. |
| `401` | Falta el token, o está vencido o es inválido (ver [Identificación del cliente](#identificación-del-cliente)). |
| `403` | Autenticado pero sin permiso. Hoy ningún endpoint lo devuelve; si aparece, usa el mismo envoltorio. |
| `404` | El recurso pedido, o alguno referenciado en el body, no existe. |
| `409` | Conflicto de stock: alguien compró antes y ya no alcanza. |
| `500` | Error inesperado del servidor. |

---

## Flujo del wizard de compra

El wizard vive en `/booking/[id]/...` y arma el paquete en memoria. Solo al confirmar le pega al backend para comprar.

| Paso | Endpoint | Qué guardar en memoria |
|---|---|---|
| 0. Evento elegido | `GET /events/{id}` | `idEvento`, fechas del evento, ciudad |
| 1. Hotel | `GET /events/{id}/hotels` | por cada habitación elegida: `idHabitacion`. Las fechas se calculan del evento (ver abajo) |
| 2. Entradas | `GET /events/{id}/tickets` | `idEntrada` y `cantidad` |
| 3. Vuelos | `GET /events/{id}/flights` | `idVuelo` y `cantidadPasajeros` (una ida y una vuelta, filtrando por `sentido`) |
| 4. Pago | `GET /payment-methods` | `idMetodoPago` de una tarjeta guardada, o los datos de una tarjeta nueva |
| 5. Confirmar | `POST /bookings` | La respuesta trae `codigoConfirmacion`, el total y el detalle para la pantalla de confirmación |

Recomendaciones:

- **Todos los productos son opcionales**, pero el paquete necesita al menos uno. El wizard puede permitir saltear pasos.
- **Fechas del hotel: las define el negocio, no el usuario.** El check-in es **1 día antes de `fechaInicio`** y el check-out **1 día después de `fechaFin`**. Si la carrera va de jueves a domingo, el check-in es el miércoles y el check-out el lunes. El front las calcula a partir del evento y las manda en cada ítem de `habitaciones`. El backend hoy no verifica esta regla, así que es responsabilidad del front respetarla. Ojo al calcularlas: las fechas del evento son `"YYYY-MM-DD"` sin hora; sumar y restar días sin pasar por zonas horarias (por ejemplo, operando sobre `Date.UTC`), para que no se corran un día.
- **El precio que muestra el front es orientativo**: el total real lo calcula el backend y viene en la respuesta del `POST`. Para el resumen previo se puede calcular igual: `precioUsd × cantidad` para entradas, `precioPorNocheUsd × noches` para hotel y `precioUsd × cantidadPasajeros` para vuelos.
- **Mostrar el stock y deshabilitar lo agotado** (`stockDisponible === 0` / `stockAsientos === 0`). Aun así, entre que el usuario elige y confirma alguien puede comprar lo último: en ese caso el `POST` responde `409` y el `message` dice qué ítem se agotó. Lo ideal es volver al paso de ese ítem sin perder el resto del paquete.
- **No confiar en `estado` del evento para saber si ya pasó.** Hay eventos cargados como `Proximo` cuyas fechas ya pasaron. Usar `fechaFin`: el backend rechaza la compra si `fechaFin` es anterior a hoy.
- **Tarjetas vencidas**: `GET /payment-methods` las marca con `vencida: true`. El `POST` las rechaza, así que conviene deshabilitarlas.
- **Validar en el front antes de enviar** (cantidades ≥ 1, 4 dígitos, `MM/AA`, al menos un ítem). Los errores de validación de campos del backend (`400` con `"Datos inválidos: ..."`) traen mensajes técnicos y en inglés, pensados para debug y no para el usuario. En cambio, los `message` de reglas de negocio (`409` de stock, evento finalizado, tarjeta vencida, etc.) están en español y se pueden mostrar tal cual.
- **Nombres de lugares**: mostrar lo que devuelve la API, no valores hardcodeados. Algunos nombres en la base no llevan tilde (por ejemplo, la ciudad figura como `"Sao Paulo"`).

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
    record: string | null;
    velocidad_maxima: string | null;
    maximo_ganador: string | null;
    circuit_svg_url: string | null;
    capacidad: string | null;
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

**Headers**: `Authorization: Bearer <token>` (ver [Identificación del cliente](#identificación-del-cliente)).

**Respuesta `200`**: `ApiResponse<MetodoPago[]>`

```ts
interface MetodoPago {
  idMetodoPago: string;
  tipo: "Credito" | "Debito";
  ultimos4Digitos: string;
  fechaExpiracion: string;   // "MM/AA"
  vencida: boolean;          // true si ya venció; el checkout la rechaza
  nombre_titular?: string;   // nombre del titular de la tarjeta
  telefono?: string;         // teléfono del cliente asociado
  dni?: number;              // DNI del cliente asociado
}
```

El tipo `Credito`/`Debito` no indica la marca (Visa, Mastercard); hoy la base no guarda la marca.

**Errores**: `401` sin token o con token vencido o inválido · `404` si el cliente no existe.

---

## Usuarios y Perfil

### `GET /users/profile`

Devuelve los datos del perfil del cliente autenticado.

**Headers**: `Authorization: Bearer <token>`

**Respuesta `200`**: `ApiResponse<UserProfile>`

```ts
interface UserProfile {
  idCliente: string;
  nombre: string;
  apellido: string;
  email: string;
  telefono: string | null;
  dni: number | null;
}
```

**Errores**: `401` sin token o con token vencido · `404` si el cliente no existe.

### `PUT /users/profile`

Actualiza (o crea si no existiera) los datos del perfil del usuario autenticado (nombre, apellido, teléfono, DNI). Soporta también la ruta `/profile`.

**Headers**: `Authorization: Bearer <token>`

**Body**

```json
{
  "nombre": "string",
  "apellido": "string",
  "telefono": "string | null",
  "dni": "number | null"
}
```

**Respuesta `200`**: `ApiResponse<UserProfile>`

```json
{
  "success": true,
  "message": "Perfil actualizado correctamente",
  "data": {
    "idCliente": "99999999-0000-4000-8000-000000000001",
    "nombre": "Juan",
    "apellido": "Perez",
    "email": "juan.perez@example.com",
    "telefono": "+54 11 5555-1234",
    "dni": 35123456
  }
}
```

**Errores**:
- `400` si `nombre` o `apellido` están vacíos.
- `401` sin token o con sesión inválida.

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
| `Authorization` | sí | `Bearer <token>` del cliente que compra (ver [Identificación del cliente](#identificación-del-cliente)). |
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
- `fechaCheckOut` tiene que ser posterior a `fechaCheckIn`, y `fechaCheckIn` no puede ser anterior a hoy. Por regla de negocio, deben ser `fechaInicio − 1 día` y `fechaFin + 1 día` del evento (ver [Flujo del wizard](#flujo-del-wizard-de-compra)).
- **No se mandan precios ni totales**: los calcula el backend.
- Con una tarjeta nueva, **el número completo nunca viaja al backend**: solo los últimos 4 dígitos y el token de la pasarela.

Ejemplo (Sao Paulo, 6–8 nov → check-in 5 nov y check-out 9 nov; 2 personas, tarjeta guardada):

```json
{
  "idEvento": "c25c1812-9ced-4f15-ad77-842e52214f05",
  "entradas": [{ "idEntrada": "55555555-0000-4000-8000-000000000017", "cantidad": 2 }],
  "habitaciones": [{ "idHabitacion": "843e9b11-1365-477a-8b69-2a8b167021ec", "fechaCheckIn": "2026-11-05", "fechaCheckOut": "2026-11-09" }],
  "vuelos": [
    { "idVuelo": "88888888-0000-4000-8000-000000000007", "cantidadPasajeros": 2 },
    { "idVuelo": "88888888-0000-4000-8000-000000000009", "cantidadPasajeros": 2 }
  ],
  "pago": { "idMetodoPago": "aaaaaaaa-0000-4000-8000-000000000001" }
}
```

**Respuesta `201`**: `ApiResponse<Reserva>`, con `message: "Reserva confirmada"`. Es la **misma forma** que devuelven `GET /bookings` y `GET /bookings/{idReserva}`:

```ts
interface Reserva {
  idReserva: string;
  codigoConfirmacion: string;  // "GP-XXXXXXXX", para mostrarle al usuario
  estado: "Pendiente" | "Pagada" | "Cancelada";  // el POST siempre devuelve "Pagada"
  fechaCompra: string;         // ISO-8601
  totalUsd: number;
  incluyeEntrada: boolean;
  incluyeHotel: boolean;
  incluyeVuelo: boolean;
  idMetodoPago: string | null; // si se pagó con tarjeta nueva, es el id con el que quedó guardada
  metodoPago: {                // nuevo. Nunca incluye el token de la pasarela
    idMetodoPago: string;
    tipo: "Credito" | "Debito";
    ultimos4Digitos: string;
  } | null;
  evento: {                    // nuevo. null si la reserva es anterior al cambio y no se pudo deducir el evento
    idEvento: string;
    temporada: number;
    fechaInicio: string;       // "YYYY-MM-DD"
    fechaFin: string;          // "YYYY-MM-DD"
    circuito: {
      nombre: string;
      ciudad: {
        nombre: string;
        pais: { nombre: string; codigoIso: string };
      };
    };
  } | null;
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
    idHotel: string;           // nuevo
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

Los campos marcados como nuevos se agregaron sin cambiar los existentes. Los precios unitarios y subtotales son **los de la compra**, no los precios actuales: si después cambia el precio de una tribuna, la reserva sigue mostrando lo que se pagó. En el `POST`, `idMetodoPago`, `metodoPago` y `evento` siempre vienen completos.

Para el ejemplo de arriba: 2 × 520 (entradas) + 4 noches × 222.75 (Doble del Grand Mercure) + 2 × 320 + 2 × 310 (vuelos) = `totalUsd: 3191.00`. El id de la habitación es del microservicio de hoteles y puede cambiar (ver [Datos de prueba](#datos-de-prueba)).

**Errores**

| Código | Casos |
|---|---|
| `400` | Body mal formado · campo inválido (el `message` lista los campos, ej. `"Datos inválidos: entradas[0].cantidad: must be greater than 0"`) · paquete vacío · evento finalizado o con `fechaFin` pasada · entrada de otro evento · hotel o vuelo fuera de la ciudad del evento · vuelo ya partido · fechas de hotel inválidas · tarjeta vencida o datos de tarjeta incompletos. |
| `401` | Sin token o con token vencido o inválido. |
| `404` | No existe el cliente, el evento, alguna entrada, habitación o vuelo, o el `idMetodoPago` no es del cliente. |
| `409` | Sin stock suficiente. El `message` indica cuál, ej. `"No hay stock suficiente para la tribuna Paddock Club"`. |

### `GET /bookings`

Reservas del cliente, de la más nueva a la más vieja (`fechaCompra` descendente). Incluye todas, cualquiera sea su `estado`; el front decide cuáles mostrar.

**Headers**: `Authorization: Bearer <token>` (ver [Identificación del cliente](#identificación-del-cliente)).

**Respuesta `200`**: `ApiResponse<Reserva[]>`, con la misma forma de `Reserva` que el [`POST /bookings`](#post-bookings). Si el cliente no tiene reservas, `data` es `[]`.

Recomendaciones para la sección "Reservas" del perfil:

- **Próximas y pasadas**: separarlas con `evento.fechaFin` (pasada si es anterior a hoy), no con `estado`. Si `evento` es `null` (reservas viejas que no se pudieron asociar), usar la fecha más tardía entre `fechaCheckOut` de las habitaciones y `fechaLlegada` de los vuelos. Las fechas `"YYYY-MM-DD"` se comparan como texto, sin pasar por `Date`, para que no se corran un día.
- **Tarjeta**: nombre del Gran Premio con `evento.circuito.nombre`, ciudad y país con `evento.circuito.ciudad`, y fechas con `evento.fechaInicio` y `evento.fechaFin`. Qué incluye, con `incluyeEntrada`, `incluyeHotel` e `incluyeVuelo`. Para el detalle corto: `entradas[i].nombreTribuna` × `cantidad`, `habitaciones[i].hotel` + `cantidadNoches` y `vuelos[i].aerolinea` + `origen` → `destino`. El total es `totalUsd`.
- **"Ver itinerario completo"**: la lista ya trae todo el detalle, así que se puede abrir sin otra llamada. `GET /bookings/{idReserva}` sirve para entrar directo por URL o para refrescar.

Ejemplo (una reserva con el paquete del ejemplo del `POST`):

```json
{
  "success": true,
  "message": "Reservas obtenidas correctamente",
  "data": [
    {
      "idReserva": "4f1c2a9e-8b7d-4c3a-9e21-7a5b6c4d3e2f",
      "codigoConfirmacion": "GP-V83PRJ6T",
      "estado": "Pagada",
      "fechaCompra": "2026-09-28T19:37:41.787325Z",
      "totalUsd": 3191.00,
      "incluyeEntrada": true,
      "incluyeHotel": true,
      "incluyeVuelo": true,
      "idMetodoPago": "aaaaaaaa-0000-4000-8000-000000000001",
      "metodoPago": { "idMetodoPago": "aaaaaaaa-0000-4000-8000-000000000001", "tipo": "Credito", "ultimos4Digitos": "4242" },
      "evento": {
        "idEvento": "c25c1812-9ced-4f15-ad77-842e52214f05",
        "temporada": 2026,
        "fechaInicio": "2026-11-06",
        "fechaFin": "2026-11-08",
        "circuito": {
          "nombre": "Autódromo José Carlos Pace (Interlagos)",
          "ciudad": { "nombre": "Sao Paulo", "pais": { "nombre": "Brasil", "codigoIso": "BR" } }
        }
      },
      "entradas": [
        { "idEntrada": "55555555-0000-4000-8000-000000000017", "nombreTribuna": "Arquibancada A", "tipo": "Asiento Numerado", "cantidad": 2, "precioUnitarioUsd": 520.00, "subtotalUsd": 1040.00 }
      ],
      "habitaciones": [
        { "idHabitacion": "843e9b11-1365-477a-8b69-2a8b167021ec", "idHotel": "36888b5a-398e-4707-96c4-98f543183ea6", "hotel": "Grand Mercure São Paulo Interlagos", "tipo": "Doble", "fechaCheckIn": "2026-11-05", "fechaCheckOut": "2026-11-09", "cantidadNoches": 4, "precioPorNocheUsd": 222.75, "subtotalUsd": 891.00 }
      ],
      "vuelos": [
        { "idVuelo": "88888888-0000-4000-8000-000000000007", "aerolinea": "Aerolíneas Argentinas", "origen": "Buenos Aires", "destino": "Sao Paulo", "fechaSalida": "2026-11-05T12:00:00Z", "fechaLlegada": "2026-11-05T15:00:00Z", "cantidadPasajeros": 2, "precioUnitarioUsd": 320.00, "subtotalUsd": 640.00 },
        { "idVuelo": "88888888-0000-4000-8000-000000000009", "aerolinea": "Aerolíneas Argentinas", "origen": "Sao Paulo", "destino": "Buenos Aires", "fechaSalida": "2026-11-09T19:00:00Z", "fechaLlegada": "2026-11-09T22:00:00Z", "cantidadPasajeros": 2, "precioUnitarioUsd": 310.00, "subtotalUsd": 620.00 }
      ]
    }
  ]
}
```

**Errores**

| Código | Casos |
|---|---|
| `401` | Sin token (`"Se requiere iniciar sesión para acceder a este recurso"`) o con token vencido o inválido (`"La sesión es inválida o expiró"`). |
| `404` | El cliente no existe (`"Cliente no encontrado"`). |

### `GET /bookings/{idReserva}`

Una reserva del cliente, con la misma forma que cada ítem de `GET /bookings`: entradas, habitaciones con fechas y noches, vuelos con horarios y el medio de pago usado.

**Headers**: `Authorization: Bearer <token>`.

**Respuesta `200`**: `ApiResponse<Reserva>`, con `message: "Reserva obtenida correctamente"`.

**Errores**

| Código | Casos |
|---|---|
| `400` | `idReserva` no es UUID (`"Valor inválido para idReserva"`). |
| `401` | Sin token o con token vencido o inválido. |
| `404` | El cliente no existe (`"Cliente no encontrado"`) · la reserva no existe **o es de otro cliente** (`"Reserva no encontrada"`, sin distinguir entre los dos casos para no revelar que la reserva existe). |

Los `message` de estos errores están en español y se pueden mostrar tal cual.

---

## Endpoints que todavía no existen

- **Alta y baja de tarjetas** (`AddPaymentModal` del perfil). Hoy una tarjeta nueva solo se guarda al usarla en una compra.
- **Datos del cliente** (`DatosView` del perfil). El registro y el login los hace el front directo contra Supabase Auth; el backend no tiene endpoints de auth.
- **Traslados**: no existen en la base (ver [`checkout.md`](./checkout.md#limitaciones-conocidas)).

---

## Datos de prueba

Hay dos orígenes de datos:

- **Hoteles y habitaciones** los carga el **microservicio de hoteles** del equipo. Sus UUIDs los genera ese servicio y pueden cambiar si se recargan, así que **no hardcodearlos**: obtenerlos siempre con [`GET /events/{id}/hotels`](#get-eventsidhotels). Hoy hay 3 hoteles por ciudad de evento, cada uno con habitaciones `Single`, `Doble` y `Suite`.
- **Todo lo demás** (entradas, vuelos, clientes, tarjetas y Buenos Aires como origen de los vuelos) lo carga [`src/main/resources/db/seed.sql`](../src/main/resources/db/seed.sql) con ids fijos. Países, ciudades, circuitos y eventos ya estaban cargados en Supabase.

**Clientes**

Para probar los endpoints con token hace falta un usuario real: registrarse desde el front (el trigger le crea la fila en `clientes`, sin tarjetas guardadas) y usar el `access_token` de la sesión. Los clientes del seed no tienen usuario en `auth.users`, así que no se puede iniciar sesión con ellos; sirven como "otro cliente" en los casos de error.

| id | Cliente | Tarjetas guardadas (`idMetodoPago`) |
|---|---|---|
| `99999999-0000-4000-8000-000000000001` | Cliente Prueba | `aaaaaaaa-0000-4000-8000-000000000001` (Crédito 4242), `aaaaaaaa-0000-4000-8000-000000000002` (Débito 8812) |
| `99999999-0000-4000-8000-000000000002` | Ana Pilotti | `aaaaaaaa-0000-4000-8000-000000000003` (Crédito 1111) |

**Eventos 2026, entradas y vuelos**

Entradas y vuelos usan UUIDs con prefijo por tabla y el número al final: entrada `017` = `55555555-0000-4000-8000-000000000017`, vuelo `007` = `88888888-0000-4000-8000-000000000007`.

| GP | `idEvento` | Fechas | Entradas | Vuelos ida / vuelta |
|---|---|---|---|---|
| Madrid | `992ae124-3d59-4adb-9fd2-f0e825c605e8` | 11–13 sep (ya pasó) | 001, 002, 003 | — |
| Bakú | `f1a34d7f-3f76-446b-bed9-22fced10166e` | 25–27 sep (ya pasó) | 004, 005, 006 | — |
| Singapur | `28780217-2728-4321-865d-db4de3d9ec26` | 9–11 oct | 007, 008, 009 | 001 / 002 |
| Austin | `b3ebbdfa-a64b-4e3f-a0a8-782af0b5b472` | 23–25 oct | 010, 011, 012 | 003 / 004 |
| Ciudad de México | `9f2762ef-6bde-4ee4-8c43-2a701e887162` | 30 oct – 1 nov | 013, 014, 015 | 005 / 006 |
| Sao Paulo | `c25c1812-9ced-4f15-ad77-842e52214f05` | 6–8 nov | 016, 017, 018 | 007, 008 / 009 |
| Las Vegas | `0807f206-e3b8-4855-9ea7-2f8b8db1cfb7` | 19–21 nov | 019, 020, 021 | 010 / 011 |
| Lusail | `4513e753-0b39-4dbd-84a1-01eb594cbf09` | 27–29 nov | 022, 023, 024 | 012 / 013 |
| Abu Dabi | `691efae9-acc9-4c45-95dc-210acf720d83` | 4–6 dic | 025, 026, 027 | 014 / 015 |

En cada evento las entradas van en orden General, Asiento Numerado y VIP. Todos los vuelos salen de Buenos Aires o vuelven ahí. Madrid y Bakú no tienen vuelos porque sus fechas ya pasaron; siguen figurando como `Proximo` en la base.

**Casos preparados para probar errores**

| Caso | Datos | Resultado esperado |
|---|---|---|
| Entrada agotada | entrada `021` (Paddock Club, Las Vegas) | `409` |
| Último asiento | vuelo `008` (LATAM a Sao Paulo, 1 asiento): la primera compra de 1 pasajero pasa, la segunda da `409` | `201`, luego `409` |
| Evento ya pasado | evento Madrid con entrada `001` | `400` |
| Vuelo ya partido | vuelo `016` (a Sao Paulo, 1 sep) con el evento Sao Paulo. No aparece en `GET /flights` | `400` |
| Entrada de otro evento | entrada `001` (Madrid) con el evento Sao Paulo | `400` |
| Hotel de otra ciudad | una habitación de `GET /events/{idMadrid}/hotels` con el evento Sao Paulo | `400` |
| Tarjeta de otro cliente | cualquier usuario logueado pagando con `idMetodoPago` `aaaaaaaa-0000-4000-8000-000000000003` (de Ana Pilotti) | `404` |
| Sin sesión | `GET /bookings` sin header `Authorization` | `401` |

Hoy no hay ninguna habitación agotada cargada. Para probar el `409` de hotel hay que elegir una habitación con poco stock (varias Suites tienen `stockDisponible: 1`) y comprarla dos veces.

Las compras de prueba descuentan stock de verdad. Para volver al estado inicial hay que devolver el stock y borrar las reservas creadas (ver el SQL de limpieza en [`checkout.md`](./checkout.md#limpiar-compras-de-prueba)).
