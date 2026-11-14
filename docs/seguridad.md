# Seguridad: qué se cambió y por qué

Repaso de seguridad del backend y el frontend (noviembre 2026), tomando como referencia OWASP y los criterios del Eje D del TP. Todo está desplegado y verificado en producción.

## Lo que cambió

| # | Problema | Qué se hizo |
|---|---|---|
| 1 | La contraseña de la base de Supabase estaba escrita en `application.properties` (y en el historial de git). | Se rotó la contraseña y se sacó del código. Ahora sale solo de la variable `SPRING_DATASOURCE_PASSWORD`. |
| 2 | `/notifications/**` era público: cualquiera podía leer las notificaciones de otro usuario con `?userId=`, marcarlas como leídas o crear notificaciones globales con links a cualquier sitio. | Ahora piden token y el usuario sale del JWT. `POST /notifications` crea solo para uno mismo y `urlDestino` tiene que ser una ruta interna. El controller de demo (`/notifications/events/*`) solo existe con perfil `dev` o `local`. |
| 3 | CORS aceptaba cualquier origen, con credenciales. | Solo se acepta el front (`grand-prix-tracker-web.vercel.app`) y `localhost:3000`, sin credenciales. |
| 4 | Los errores 500 devolvían el mensaje interno de la excepción (SQL, URLs de otros servicios). | El 500 devuelve un mensaje genérico y el detalle va solo al log. Si el microservicio de ticketing falla, se responde 503. |
| 5 | El front escribía directo en la tabla `clientes` de Supabase, salteando la validación de la API. | El perfil se lee y se guarda solo por `PUT /users/profile`. RLS está activo en todas las tablas y no hay políticas de escritura. |
| 6 | Casi no había validación de entrada. | Validación de largo y formato en perfil, métodos de pago y notificaciones. En el checkout se agregaron topes: 10 entradas por tribuna, 10 pasajeros por vuelo, 10 ítems de cada tipo y 30 noches. |
| 7 | Faltaban headers de seguridad. | La API y el front envían CSP, `X-Frame-Options`, `X-Content-Type-Options`, `Referrer-Policy` y HSTS. El front envía además `Permissions-Policy`. |
| 8 | Se logueaba el email del cliente. | Los logs no incluyen datos personales; se identifica la compra por el código de reserva. |
| 9 | Había dependencias con vulnerabilidades conocidas (Tomcat, Jackson, Next.js). | Spring Boot pasó a 4.1.2 y Next.js a 16.4.6. Backend: 0 vulnerabilidades (escaneo OSV). Front: 0 vulnerabilidades (`pnpm audit`). |

Además se verificó, con un test, que el endpoint SOAP rechaza XXE: no resuelve entidades externas ni se le puede inyectar una expansión de entidades ("billion laughs").

## Lo que cada uno tiene que saber

- **Para correr el backend contra Supabase** hay que definir `SPRING_DATASOURCE_PASSWORD`. La contraseña se pide por privado; nunca va en el código ni en un archivo commiteado. Sin Supabase, se puede usar el perfil `local` con `docker compose up`.
- **Si el front corre en otro puerto u otro dominio**, hay que agregarlo en `CORS_ALLOWED_ORIGINS` (lista separada por comas). Si no, el navegador bloquea las llamadas.
- **Los datos se modifican solo por la API.** No agregar políticas de escritura en Supabase ni hacer `supabase.from(...).update(...)` desde el front.
- **Todo endpoint nuevo exige token por defecto.** Para hacerlo público hay que agregarlo a propósito en `SecurityConfig`. El usuario siempre se toma del JWT (`sub`), nunca de un parámetro o del body.
- **Si el front carga recursos de un dominio externo nuevo** (imágenes, APIs, scripts), hay que agregarlo a la CSP en `next.config.ts`. Si no, el navegador lo bloquea.

## Qué quedó afuera, y por qué

- **Rate limiting en la API:** el login lo maneja Supabase Auth, que ya limita los intentos y responde 429. En la API no hay credenciales que adivinar por fuerza bruta.
- **Contenedor sin usuario root:** mejora poco en Render y el TP no lo evalúa.
- **Pipeline de CI con escaneo automático de secretos y dependencias:** queda como tarea aparte. Hoy los escaneos se corren a mano.

## Dónde está la evidencia

- Tests: `SecurityConfigTest`, `NotificationControllerTest`, `GlobalExceptionHandlerTest`, `SoapXxeTest` y los tests de validación de `UserControllerTest`, `PaymentControllerTest` y `BookingControllerTest`.
- Detalle de los endpoints y códigos de respuesta: [`api.md`](./api.md).
- Estado de RLS en la base: [`db.md`](./db.md).
