# GrandPrix Tracker — Backend Core
## Documentación Técnica de Arquitectura de API REST (Nivel 2 de Richardson & OpenAPI 3.1.0)

**Materia:** Arquitectura de Software — Actividad Integradora  
**Rol:** Software Architect & Lead Technical Writer  
**Proyecto:** GrandPrix Tracker (Backend Core)  
**Versión del Documento:** 1.0.0  
**Fecha de Emisión:** Octubre 2026  

---

## 1. Análisis del Contexto y Arquitectura General

El sistema **GrandPrix Tracker (Backend Core)** es el componente central y orquestador del ecosistema de la plataforma. Cumple el rol de motor transaccional y proveedor de datos para el cliente web SPA/SSR desarrollado en **Next.js**.

```
+----------------------------------------------------------------------------------------------------+
|                                      ECOSISTEMA GRANDPRIX TRACKER                                   |
+----------------------------------------------------------------------------------------------------+

   +-----------------------+
   |   Frontend Web        |
   |   (Next.js / SSR)     |
   +-----------+-----------+
               |
               | HTTPS / REST (JSON) + JWT (Supabase Auth)
               v
+----------------------------------------------------------------------------------------------------+
| GRANDPRIX TRACKER — BACKEND CORE (Spring Boot 3.x / Java 21)                                       |
|                                                                                                    |
|  [Security Filter Chain]  --> Valida JWT (ES256 JWKS) & Claims (sub = id_cliente)                 |
|                                                                                                    |
|  [Controladores REST]     --> API v1 (/v1/reservas, /v1/catalogos, etc.)                           |
|                                                                                                    |
|  [CheckoutFacade]         --> Transacción Única (@Transactional ACID)                              |
|                               |                                                                    |
|                               +---> Catálogo Interno: Hoteles, Vuelos, Circuitos                   |
|                               |                                                                    |
|                               +---> Integración SOAP Síncrona: Ticketing F1 (Reserva atómica)      |
|                               |                                                                    |
|                               +---> Pasarela de Pagos (Validación & Débito)                        |
|                                                                                                    |
|  [ApplicationEventPub.]   --> Patrón Observer (Desacoplamiento asíncrono)                         |
|                               |                                                                    |
|                               +---> Notificaciones In-App (DB Supabase)                           |
|                               +---> Emails Transaccionales (@Async Brevo API post-commit)          |
+----------------------------------------------------------------------------------------------------+
        |                                |                                   |
        | JPA / HikariCP                 | REST / SOAP Wrapper               | Eventos Asíncronos
        v                                v                                   v
+-----------------------+  +---------------------------------+  +-------------------------+
| PostgreSQL (Supabase) |  | Microservicio Ticketería F1     |  | Brevo Email Sender      |
| - Eventos & Circuitos |  | (microservice_ticketing_gp)     |  | (API REST Externa)      |
| - Hoteles & Vuelos    |  | Interfaz SOAP / WSDL Legado     |  | Envíos en hilo separado |
| - Reservas & Detalles |  | Bloqueo síncrono de inventario  |  | post-confirmación       |
+-----------------------+  +---------------------------------+  +-------------------------+
```

### Características Arquitectónicas Clave:
1. **API REST Nivel 2 de Richardson:**
   - **Nivel 1 (Recursos):** Cada entidad del dominio cuenta con una URI sustantiva y jerárquica clara (`/v1/reservas`, `/v1/reservas/{id}`, `/v1/catalogos/{codigoGP}`).
   - **Nivel 2 (Verbos y Códigos HTTP):** Uso riguroso de verbos estándar (`GET`, `POST`, `PATCH`, `DELETE`) y códigos de estado semánticos (`200 OK`, `201 Created` con header `Location`, `204 No Content`, `400 Bad Request`, `401 Unauthorized`, `404 Not Found`, `409 Conflict`, `504 Gateway Timeout`), estandarizando los errores bajo la norma **RFC 9457 (Problem Details)**.
2. **Persistencia Relacional y Transaccionalidad:**
   - Conexión a base de datos **PostgreSQL en Supabase** mediante JPA/Hibernate y un pool administrado por **HikariCP** configurado para cargas de alta concurrencia.
   - Orquestación en una transacción de base de datos única (`@Transactional`) mediante el patrón **Facade (`CheckoutFacade`)**, garantizando semántica *todo o nada* (atomicidad) en compras compuestas (entradas + hotel + vuelos + traslados).
3. **Integración Síncrona con el Sistema Legado (SOAP Ticketing F1):**
   - Durante el checkout, el Core interactúa de forma síncrona mediante un cliente especializado (`TicketingMicroserviceClient`) que se comunica con el sistema legado de F1 (`microservice_ticketing_gp`).
   - Esta llamada asegura el bloqueo físico de las butacas antes de confirmar el paquete turístico. Se gobierna mediante políticas estrictas de timeouts (5 segundos) y mapeo defensivo de excepciones.
4. **Desacoplamiento Asíncrono mediante Eventos (Patrón Observer):**
   - Utilización de `ApplicationEventPublisher` para notificar internamente eventos de dominio como `CompraConfirmadaEvent`.
   - Los consumidores de notificaciones (`NotificationEventListener`) y el despachador de emails (`CompraConfirmadaEmailListener`) operan con `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` y `@Async`, garantizando que fallas de proveedores de mensajería (ej. Brevo) jamás reviertan una compra exitosa ni degraden la latencia de respuesta al usuario.

---

## 2. Entregable 1: Catálogo de Endpoints REST (Nivel 2 de Richardson)

A continuación se define el catálogo técnico con los 6 endpoints clave de la API Core, incorporando versionado en la URI (`/v1/...`):

| Método | URI | Descripción Breve | Headers Principales | Códigos de Respuesta Esperados |
| :--- | :--- | :--- | :--- | :--- |
| **POST** | `/v1/reservas` | Crea una nueva reserva de paquete turístico de F1 (entradas, alojamiento, vuelos y traslados) orquestando la reserva síncrona de tickets vía SOAP y la deducción atómica de inventario local. | `Authorization: Bearer <JWT>`<br>`Idempotency-Key: <UUID>`<br>`Content-Type: application/json` | `201 Created` (incluye header `Location`)<br>`400 Bad Request`<br>`401 Unauthorized`<br>`404 Not Found`<br>`409 Conflict`<br>`504 Gateway Timeout` |
| **GET** | `/v1/reservas/{id}` | Recupera la información detallada y el estado actual de una reserva puntual (`idReserva`), garantizando que pertenezca exclusivamente al cliente autenticado. | `Authorization: Bearer <JWT>`<br>`Accept: application/json` | `200 OK`<br>`401 Unauthorized`<br>`404 Not Found` |
| **DELETE** | `/v1/reservas/{id}` | Cancela una reserva existente, dispara la restitución de cupos (hoteles y vuelos en Postgres) y emite la transacción de compensación hacia el sistema legado de tickets vía SOAP. | `Authorization: Bearer <JWT>` | `204 No Content`<br>`401 Unauthorized`<br>`404 Not Found`<br>`409 Conflict` |
| **GET** | `/v1/catalogos/{codigoGP}` | Consulta unificada del catálogo consolidado para un Gran Premio: información del circuito, fechas, disponibilidad de tribunas, opciones de alojamiento en la ciudad y vuelos conexos. | `Accept: application/json` | `200 OK`<br>`404 Not Found` |
| **PATCH** | `/v1/reservas/{id}/pago` | Completa o actualiza el estado de una reserva previamente generada en condición pendiente, imputando un método de pago válido y pasando el estado a `PAGADA`. | `Authorization: Bearer <JWT>`<br>`Content-Type: application/json` | `200 OK`<br>`400 Bad Request`<br>`401 Unauthorized`<br>`404 Not Found`<br>`409 Conflict` |
| **GET** | `/v1/notificaciones` | Obtiene el listado cronológico de notificaciones internas del usuario autenticado (confirmaciones de compra, recordatorios de viaje o cambios de estado). Permite filtrar por leídas/no leídas. | `Authorization: Bearer <JWT>`<br>`Accept: application/json` | `200 OK`<br>`401 Unauthorized` |

---

### Detalle de Especificación de Endpoints

#### 1. Crear Reserva de Paquete Turístico
- **Método y Ruta:** `POST /v1/reservas`
- **Propósito:** Ejecuta el checkout coordinado. Valida los cupos de hotel y vuelos en PostgreSQL, llama de manera síncrona al servicio SOAP de ticketería para apartar las entradas en la tribuna elegida, persiste la reserva en estado `PAGADA` y publica el evento para notificaciones posteriores.
- **Códigos HTTP:**
  - `201 Created`: Reserva creada y confirmada. Incluye el header `Location: /v1/reservas/{id}` y el objeto de la reserva en el cuerpo.
  - `400 Bad Request`: Payload malformado, parámetros faltantes o fechas incompatibles (ej. check-out anterior a check-in).
  - `401 Unauthorized`: Token JWT ausente, caducado o con firma no reconocida.
  - `404 Not Found`: No existe el Gran Premio, el hotel, la habitación o el vuelo indicado.
  - `409 Conflict`: Conflicto de concurrencia o stock agotado (tanto local como devuelto por el servicio SOAP de tickets).
  - `504 Gateway Timeout`: El sistema externo de tickets no respondió dentro del límite estricto de 5 segundos.

#### 2. Consultar Reserva Específica
- **Método y Ruta:** `GET /v1/reservas/{id}`
- **Propósito:** Recupera el desglose completo del itinerario: datos del Gran Premio, entradas con tribuna y asiento, fechas de estancia en hotel, detalles del vuelo y monto total abonado en USD. Si la reserva no pertenece al usuario emisor del JWT, responde `404` para evitar ataques de enumeración de recursos (*security through isolation*).
- **Códigos HTTP:**
  - `200 OK`: Datos completos de la reserva.
  - `401 Unauthorized`: Sesión inválida.
  - `404 Not Found`: Reserva inexistente o de otro propietario.

#### 3. Cancelar Reserva
- **Método y Ruta:** `DELETE /v1/reservas/{id}`
- **Propósito:** Marca la reserva como `CANCELADA` y revierte los inventarios. Ejecuta llamadas de anulación al proveedor SOAP de ticketería y recompone el stock de habitaciones y vuelos.
- **Códigos HTTP:**
  - `204 No Content`: Cancelación ejecutada con éxito (cuerpo vacío).
  - `401 Unauthorized`: No autorizado.
  - `404 Not Found`: La reserva solicitada no existe.
  - `409 Conflict`: La reserva ya se encuentra cancelada o el evento ya comenzó, impidiendo la cancelación por política de negocio.

#### 4. Consultar Catálogo Unificado de un Gran Premio
- **Método y Ruta:** `GET /v1/catalogos/{codigoGP}`
- **Propósito:** Facilita al frontend la carga íntegra del catálogo del Gran Premio (ej. `MONZA-2026` o UUID) combinando circuito, tribunas con disponibilidad, hoteles disponibles en la ciudad sede y vuelos programados.
- **Códigos HTTP:**
  - `200 OK`: Catálogo consolidado retornado en formato JSON.
  - `404 Not Found`: El código de evento o temporada no fue encontrado en Supabase.

#### 5. Pagar / Completar Reserva Pendiente
- **Método y Ruta:** `PATCH /v1/reservas/{id}/pago`
- **Propósito:** En escenarios donde la reserva queda reservada condicionalmente (estado `PENDIENTE_PAGO`), permite imputar una tarjeta de crédito/débito para consumar el cobro y ratificar la compra.
- **Códigos HTTP:**
  - `200 OK`: Pago procesado y reserva en estado `PAGADA`.
  - `400 Bad Request`: Datos de la tarjeta inválidos o formato no soportado.
  - `401 Unauthorized`: Token no válido.
  - `404 Not Found`: Reserva no hallada.
  - `409 Conflict`: La reserva ya fue abonada previamente o la ventana de espera de pago caducó.

#### 6. Obtener Notificaciones del Usuario
- **Método y Ruta:** `GET /v1/notificaciones`
- **Propósito:** Lista los mensajes y avisos generados para el cliente mediante el patrón Observer (confirmación de compra, cambios de estado en vuelos, ofertas). Soporta parámetros de consulta como `?soloNoLeidas=true` y paginación.
- **Códigos HTTP:**
  - `200 OK`: Colección de notificaciones del cliente autenticado.
  - `401 Unauthorized`: Falta de credenciales de autenticación.

---

## 3. Entregable 2: Contrato OpenAPI 3.1.0 (YAML)

El siguiente contrato en formato OpenAPI 3.1.0 modela de manera estricta los dos endpoints críticos: `POST /v1/reservas` y `GET /v1/reservas/{id}`, incorporando el estándar **Problem Details (RFC 9457)** para todas las respuestas de error.

```yaml
openapi: 3.1.0
info:
  title: GrandPrix Tracker Core API
  version: 1.0.0
  description: >
    API REST (Nivel 2 de Richardson) del Backend Core de GrandPrix Tracker.
    Gestiona el catálogo de Grandes Premios de F1, la adquisición de paquetes
    turísticos integrados (entradas, hoteles y vuelos) y la orquestación síncrona
    con el servicio legado de ticketería mediante SOAP.
servers:
  - url: https://grand-prix-tracker-api.onrender.com/v1
    description: Servidor de Producción (Render)
  - url: http://localhost:8080/v1
    description: Servidor Local de Desarrollo

paths:
  /reservas:
    post:
      summary: Crear reserva de paquete turístico de Fórmula 1
      description: >
        Procesa el checkout integral del paquete. Coordina la deducción atómica de
        inventario local y la llamada síncrona al servicio SOAP de ticketería.
        Garantiza idempotencia mediante el encabezado `Idempotency-Key`.
      operationId: crearReserva
      security:
        - bearerAuth: []
      parameters:
        - name: Idempotency-Key
          in: header
          required: false
          description: Identificador UUID v4 único generado por el cliente para evitar compras duplicadas por timeouts.
          schema:
            type: string
            format: uuid
            example: "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d"
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CheckoutRequest'
      responses:
        '201':
          description: Reserva creada y confirmada exitosamente.
          headers:
            Location:
              description: URI del recurso de reserva recién creado.
              schema:
                type: string
                example: "/v1/reservas/c7a4b8f2-3e1d-4876-9c2b-5e6a0d2f819a"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ReservaResponse'
        '400':
          description: Solicitud mal formada o reglas de negocio incumplidas.
          content:
            application/problem+json:
              schema:
                $ref: '#/components/schemas/ProblemDetails'
        '401':
          description: Autenticación requerida o token JWT inválido/expirado.
          content:
            application/problem+json:
              schema:
                $ref: '#/components/schemas/ProblemDetails'
        '404':
          description: Evento, entrada, hotel o vuelo no encontrado.
          content:
            application/problem+json:
              schema:
                $ref: '#/components/schemas/ProblemDetails'
        '409':
          description: Conflicto de inventario (agotado en base de datos local o en sistema SOAP legado).
          content:
            application/problem+json:
              schema:
                $ref: '#/components/schemas/ProblemDetails'
        '504':
          description: Tiempo de espera agotado al conectar con el microservicio SOAP de ticketería.
          content:
            application/problem+json:
              schema:
                $ref: '#/components/schemas/ProblemDetails'

  /reservas/{id}:
    get:
      summary: Consultar estado y detalle de una reserva
      description: >
        Obtiene los detalles del paquete turístico adquirido por el cliente autenticado.
        Si la reserva pertenece a otro usuario o no existe, responde con 404.
      operationId: obtenerReservaPorId
      security:
        - bearerAuth: []
      parameters:
        - name: id
          in: path
          required: true
          description: Identificador UUID de la reserva.
          schema:
            type: string
            format: uuid
            example: "c7a4b8f2-3e1d-4876-9c2b-5e6a0d2f819a"
      responses:
        '200':
          description: Detalle de la reserva recuperado exitosamente.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ReservaResponse'
        '401':
          description: Autenticación inválida o no provista.
          content:
            application/problem+json:
              schema:
                $ref: '#/components/schemas/ProblemDetails'
        '404':
          description: Reserva no encontrada para el cliente en sesión.
          content:
            application/problem+json:
              schema:
                $ref: '#/components/schemas/ProblemDetails'

components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
      description: Token de acceso emitido por Supabase Auth (algoritmo ES256).

  schemas:
    CheckoutRequest:
      type: object
      required:
        - idEvento
        - pago
      properties:
        idEvento:
          type: string
          format: uuid
          description: ID del Gran Premio de F1 en el catálogo.
          example: "3fa85f64-5717-4562-b3fc-2c963f66afa6"
        entradas:
          type: array
          items:
            $ref: '#/components/schemas/EntradaItem'
        habitaciones:
          type: array
          items:
            $ref: '#/components/schemas/HabitacionItem'
        vuelos:
          type: array
          items:
            $ref: '#/components/schemas/VueloItem'
        pago:
          $ref: '#/components/schemas/PagoRequest'
        incluyeTransporte:
          type: boolean
          default: false
          description: Traslados terrestres circuito-hotel (+30 USD).
          example: true

    EntradaItem:
      type: object
      required:
        - idEntrada
        - cantidad
      properties:
        idEntrada:
          type: string
          format: uuid
          example: "e5a3bc91-23d4-4861-9c1a-8b1e42f7c001"
        cantidad:
          type: integer
          minimum: 1
          example: 2

    HabitacionItem:
      type: object
      required:
        - idHabitacion
        - fechaCheckIn
        - fechaCheckOut
      properties:
        idHabitacion:
          type: string
          format: uuid
          example: "7c12f450-8b11-4ca5-9831-2917540cc8e3"
        fechaCheckIn:
          type: string
          format: date
          example: "2026-09-03"
        fechaCheckOut:
          type: string
          format: date
          example: "2026-09-07"

    VueloItem:
      type: object
      required:
        - idVuelo
        - cantidadPasajeros
      properties:
        idVuelo:
          type: string
          format: uuid
          example: "4b98c3e1-76a2-4a0b-8d76-192837465abc"
        cantidadPasajeros:
          type: integer
          minimum: 1
          example: 2

    PagoRequest:
      type: object
      properties:
        idMetodoPago:
          type: string
          format: uuid
          nullable: true
          description: ID de tarjeta guardada previamente en Supabase.
          example: "123e4567-e89b-12d3-a456-426614174000"
        nuevaTarjeta:
          $ref: '#/components/schemas/NuevaTarjetaRequest'

    NuevaTarjetaRequest:
      type: object
      required:
        - numero
        - titular
        - mesVencimiento
        - anioVencimiento
        - cvv
      properties:
        numero:
          type: string
          pattern: '^[0-9]{13,19}$'
          example: "4532015099881234"
        titular:
          type: string
          example: "CHARLES LECLERC"
        mesVencimiento:
          type: integer
          minimum: 1
          maximum: 12
          example: 11
        anioVencimiento:
          type: integer
          minimum: 2026
          example: 2029
        cvv:
          type: string
          pattern: '^[0-9]{3,4}$'
          example: "916"

    ReservaResponse:
      type: object
      required:
        - idReserva
        - codigoConfirmacion
        - estado
        - fechaCompra
        - totalUsd
      properties:
        idReserva:
          type: string
          format: uuid
          example: "c7a4b8f2-3e1d-4876-9c2b-5e6a0d2f819a"
        codigoConfirmacion:
          type: string
          example: "GP-MNZ7842K"
        estado:
          type: string
          enum: [PAGADA, PENDIENTE_PAGO, CANCELADA]
          example: "PAGADA"
        fechaCompra:
          type: string
          format: date-time
          example: "2026-10-05T14:48:22Z"
        totalUsd:
          type: number
          format: double
          example: 1880.00
        incluyeEntrada:
          type: boolean
          example: true
        incluyeHotel:
          type: boolean
          example: true
        incluyeVuelo:
          type: boolean
          example: true
        incluyeTransporte:
          type: boolean
          example: true
        evento:
          type: object
          properties:
            idEvento:
              type: string
              format: uuid
            temporada:
              type: integer
            circuito:
              type: object
              properties:
                nombre:
                  type: string
                  example: "Autodromo Nazionale Monza"
                ciudad:
                  type: string
                  example: "Monza"
                pais:
                  type: string
                  example: "Italia"
        entradas:
          type: array
          items:
            type: object
            properties:
              idEntrada:
                type: string
                format: uuid
              nombreTribuna:
                type: string
                example: "Tribuna Prima Variante"
              tipo:
                type: string
                example: "Asiento Numerado"
              cantidad:
                type: integer
                example: 2
              precioUnitarioUsd:
                type: number
                example: 450.00
              subtotalUsd:
                type: number
                example: 900.00

    ProblemDetails:
      type: object
      description: Estructura estándar de reporte de errores conforme a RFC 9457 (Problem Details for HTTP APIs).
      required:
        - type
        - title
        - status
        - detail
      properties:
        type:
          type: string
          format: uri
          description: URI que identifica y categoriza el tipo de problema.
          example: "https://grandprixtracker.uade.edu.ar/errors/inventory-conflict"
        title:
          type: string
          description: Resumen corto y legible para humanos del tipo de error.
          example: "Conflicto de Inventario de Entradas"
        status:
          type: integer
          description: Código de estado HTTP generado por el servidor de origen.
          example: 409
        detail:
          type: string
          description: Explicación humana detallada y específica de la ocurrencia del error.
          example: "El inventario de entradas para la tribuna seleccionada se encuentra agotado en el sistema oficial de Ticketería F1."
        instance:
          type: string
          format: uri-reference
          description: URI que identifica la solicitud o recurso específico que originó el problema.
          example: "/v1/reservas"
        code:
          type: string
          description: Código de negocio mnemónico para consumo programático por el frontend.
          example: "TICKET_INVENTORY_EXHAUSTED"
        timestamp:
          type: string
          format: date-time
          example: "2026-10-05T14:48:00Z"
        invalidParams:
          type: array
          description: Lista de campos específicos con fallas de validación de negocio.
          items:
            type: object
            properties:
              name:
                type: string
                example: "entradas[0].idEntrada"
              reason:
                type: string
                example: "Stock insuficiente en sistema legado F1"
```

---

## 4. Entregable 3: Ejemplos JSON

### 4.1. Request y Response Exitoso: Creación de Reserva (HTTP 201 Created)

#### Request HTTP:
```http
POST /v1/reservas HTTP/1.1
Host: grand-prix-tracker-api.onrender.com
Authorization: Bearer eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxZmEyYjNjNC01ZDZlLTdmOGEtOWIwYy0xMWQxMmUxM2YxNGEiLCJhdWQiOiJhdXRoZW50aWNhdGVkIiwiZXhwIjoxNzkxMzA0MDAwfQ.abcdef...
Idempotency-Key: 9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d
Content-Type: application/json
Accept: application/json

{
  "idEvento": "550e8400-e29b-41d4-a716-446655440000",
  "entradas": [
    {
      "idEntrada": "7a3b4c5d-6e7f-8a9b-0c1d-2e3f4a5b6c7d",
      "cantidad": 2
    }
  ],
  "habitaciones": [
    {
      "idHabitacion": "8b4c5d6e-7f8a-9b0c-1d2e-3f4a5b6c7d8e",
      "fechaCheckIn": "2026-09-03",
      "fechaCheckOut": "2026-09-07"
    }
  ],
  "vuelos": [
    {
      "idVuelo": "9c5d6e7f-8a9b-0c1d-2e3f-4a5b6c7d8e9f",
      "cantidadPasajeros": 2
    }
  ],
  "pago": {
    "nuevaTarjeta": {
      "numero": "4532015099881234",
      "titular": "CARLOS SAINZ",
      "mesVencimiento": 10,
      "anioVencimiento": 2028,
      "cvv": "789"
    }
  },
  "incluyeTransporte": true
}
```

#### Response HTTP:
```http
HTTP/1.1 201 Created
Location: /v1/reservas/c7a4b8f2-3e1d-4876-9c2b-5e6a0d2f819a
Content-Type: application/json; charset=utf-8
Date: Mon, 05 Oct 2026 14:48:22 GMT

{
  "idReserva": "c7a4b8f2-3e1d-4876-9c2b-5e6a0d2f819a",
  "codigoConfirmacion": "GP-MNZ7842K",
  "estado": "PAGADA",
  "fechaCompra": "2026-10-05T14:48:22Z",
  "totalUsd": 1930.00,
  "incluyeEntrada": true,
  "incluyeHotel": true,
  "incluyeVuelo": true,
  "incluyeTransporte": true,
  "idMetodoPago": "e1f2a3b4-c5d6-e7f8-a9b0-1c2d3e4f5a6b",
  "metodoPago": {
    "idMetodoPago": "e1f2a3b4-c5d6-e7f8-a9b0-1c2d3e4f5a6b",
    "tipo": "CREDITO",
    "ultimos4Digitos": "1234"
  },
  "evento": {
    "idEvento": "550e8400-e29b-41d4-a716-446655440000",
    "temporada": 2026,
    "fechaInicio": "2026-09-04",
    "fechaFin": "2026-09-06",
    "circuito": {
      "nombre": "Autodromo Nazionale Monza",
      "ciudad": {
        "nombre": "Monza",
        "pais": {
          "nombre": "Italia",
          "codigoIso": "IT"
        }
      }
    }
  },
  "entradas": [
    {
      "idEntrada": "7a3b4c5d-6e7f-8a9b-0c1d-2e3f4a5b6c7d",
      "nombreTribuna": "Tribuna Prima Variante",
      "tipo": "Asiento Numerado",
      "cantidad": 2,
      "precioUnitarioUsd": 450.00,
      "subtotalUsd": 900.00
    }
  ],
  "habitaciones": [
    {
      "idHabitacion": "8b4c5d6e-7f8a-9b0c-1d2e-3f4a5b6c7d8e",
      "idHotel": "11223344-5566-7788-99aa-bbccddeeff00",
      "hotel": "Grand Hotel de la Ville Monza",
      "tipo": "Deluxe King",
      "fechaCheckIn": "2026-09-03",
      "fechaCheckOut": "2026-09-07",
      "cantidadNoches": 4,
      "precioPorNocheUsd": 150.00,
      "subtotalUsd": 600.00
    }
  ],
  "vuelos": [
    {
      "idVuelo": "9c5d6e7f-8a9b-0c1d-2e3f-4a5b6c7d8e9f",
      "aerolinea": "ITA Airways",
      "origen": "Ezeiza (EZE)",
      "destino": "Milán Malpensa (MXP)",
      "fechaSalida": "2026-09-02T22:30:00Z",
      "fechaLlegada": "2026-09-03T16:15:00Z",
      "cantidadPasajeros": 2,
      "precioUnitarioUsd": 200.00,
      "subtotalUsd": 400.00
    }
  ]
}
```

---

### 4.2. Response de Error Problem Details (RFC 9457): Falla en SOAP de Ticketería (HTTP 409 Conflict)

**Contexto del Fallo:**  
Durante la ejecución del método `CheckoutFacade.checkout(...)`, el cliente síncrono `TicketingMicroserviceClient` envió la petición SOAP al sistema externo para apartar dos butacas en la tribuna *"Curva Parabólica"*. El sistema legado de ticketería devolvió el siguiente XML Fault:
```xml
<soap:Fault>
    <faultcode>soap:Client</faultcode>
    <faultstring>STOCK_EXHAUSTED: No seats available for tribuna VIP Parabolica</faultstring>
</soap:Fault>
```
El Core capturó este fallo, ejecutó el rollback de la transacción y mapeó el error a una respuesta REST **409 Conflict** estandarizada con `application/problem+json`:

#### Response HTTP:
```http
HTTP/1.1 409 Conflict
Content-Type: application/problem+json; charset=utf-8
Date: Mon, 05 Oct 2026 14:48:25 GMT

{
  "type": "https://grandprixtracker.uade.edu.ar/errors/inventory-conflict",
  "title": "Conflicto de Inventario de Entradas",
  "status": 409,
  "detail": "No se pudo concretar la reserva: las entradas seleccionadas para la tribuna 'Curva Parabolica' ya no se encuentran disponibles en el sistema oficial de Ticketería F1.",
  "instance": "/v1/reservas",
  "code": "TICKET_INVENTORY_EXHAUSTED",
  "timestamp": "2026-10-05T14:48:25Z",
  "invalidParams": [
    {
      "name": "entradas[0].idEntrada",
      "reason": "Cupos insuficientes para la cantidad solicitada (solicitado: 2, disponible: 0)"
    }
  ]
}
```

---

## 5. Entregable 4: Decisiones Justificadas y Manejo de Fallas (El Desafío de la Cátedra)

### 5.1. Idempotencia en el Checkout (`POST /v1/reservas`)
En arquitecturas distribuidas con clientes web desacoplados (Next.js) que ejecutan pagos y consumos de recursos no renovables, la red es intrínsecamente no confiable (falacia de las redes distribuidas). Si tras despachar un `POST /v1/reservas` ocurre una microdesconexión o latencia que dispara un timeout del lado del navegador antes de recibir la confirmación HTTP, el usuario o las librerías cliente (como los reintentos automáticos de TanStack Query o Axios) tienden a reintentar la operación. Para prevenir de forma estricta compras duplicadas y cobros dobles sobre la tarjeta, implementamos el patrón **Idempotency Key** mediante el encabezado HTTP estándar `Idempotency-Key: <UUIDv4>`. 

El frontend genera dicho identificador criptográfico único al inicializar el paso de confirmación y lo adjunta en la petición. En el Backend Core, un interceptor/filtro transaccional valida la clave contra un almacén atómico (PostgreSQL o clave en caché distribuida con TTL de 24 horas) antes de ingresar al `CheckoutFacade`. Si la clave ya está registrada en estado `PROCESSING`, se rechaza el intento concurrente con un `409 Conflict` ("Transacción en curso"); y si ya fue procesada exitosamente con anterioridad (`COMPLETED`), el Core intercepta la llamada y **retorna de inmediato la respuesta HTTP 201 Created original previamente cacheada con su cabecera `Location` y cuerpo JSON idéntico**, sin ejecutar nuevamente la pasarela de pagos ni interactuar de nuevo con el servicio SOAP de tickets. De esta forma, garantizamos semántica de ejecución exactamente una vez (*effectively-once delivery*) protegiendo la consistencia financiera y el inventario.

### 5.2. El Timeout del Legado SOAP de F1 (Pregunta Clave de Arquitectura)
Durante el checkout, el Backend Core orquesta de manera síncrona la reserva de tickets contra el sistema legado de F1 mediante una interfaz SOAP. En caso de que este sistema externo colapse o sufra una degradación severa superando la ventana de corte estricta de **5 segundos** (`readTimeout = 5000 ms`), el cliente HTTP subyacente aborta la conexión disparando una excepción `ResourceAccessException` / `SocketTimeoutException`. Ante este evento, el Backend Core ejecuta de inmediato dos acciones arquitectónicas fundamentales:

1. **Rollback Transaccional Atómico Local:** La transacción de Spring (`@Transactional`) que encapsula a `CheckoutFacade` detecta la excepción no verificada y ejecuta un rollback inmediato en PostgreSQL (Supabase). Ninguna reserva se persiste, se desestiman las deducciones de cupo en hoteles y vuelos, y no se ejecuta ningún cobro en la pasarela de pagos, preservando la consistencia ACID del Core.
2. **Respuesta REST Semántica Exacta:** La API REST devuelve de forma estricta el código **`504 Gateway Timeout`** (no un `502 Bad Gateway`). De acuerdo con la especificación HTTP (RFC 9110, sección 15.6.5), el código `504` describe con exactitud el rol de nuestro Core actuando como pasarela/orquestador que no recibió a tiempo la respuesta de un servidor upstream necesario para completar la solicitud (mientras que `502` correspondería si el servidor legado hubiese rechazado la conexión de forma inmediata o respondido con un XML no parseable).

Para notificar este escenario al frontend sin comprometer el encapsulamiento ni la seguridad por diseño (CWE-209), el `GlobalExceptionHandler` intercepta el timeout y genera un documento estandarizado bajo **RFC 9457 (Problem Details)**:

```json
{
  "type": "https://grandprixtracker.uade.edu.ar/errors/upstream-timeout",
  "title": "Servicio de Ticketería no disponible",
  "status": 504,
  "detail": "El servicio oficial de Ticketería de F1 no respondió dentro del límite de tiempo establecido (5 segundos). La operación fue cancelada y no se realizó ningún cargo.",
  "instance": "/v1/reservas",
  "code": "UPSTREAM_TIMEOUT",
  "timestamp": "2026-10-05T14:48:30Z"
}
```

Esta estrategia de manejo de errores garantiza:
- **Ocultamiento de Información Sensible:** Se suprimen de raíz las trazas de pila Java (`java.net.SocketTimeoutException`, nombres de paquetes, números de línea de `CheckoutFacade.java:122` o IPs internas de Render).
- **Contrato Transparente y Limpio:** Se clasifica el origen de la falla como externa (*upstream provider*) permitiendo que la interfaz de usuario en Next.js brinde un mensaje claro al usuario: *"El sistema de tickets de F1 está experimentando demoras. Tu compra no fue cobrada; por favor reintenta en unos instantes."*
