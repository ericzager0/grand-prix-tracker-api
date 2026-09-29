# Grand Prix Tracker — Backend

API REST desarrollada con **Spring Boot** y **Java**, organizada bajo una arquitectura de **componentes por feature** (Package by Feature).

---

## Punto de entrada

```
src/main/java/com/uade/grandprixtracker/GrandPrixTrackerApplication.java
```

Esta clase contiene el método `main` y la anotación `@SpringBootApplication`, que arranca toda la aplicación. Es el único punto de entrada del sistema.

```java
@SpringBootApplication
public class GrandPrixTrackerApplication {
    public static void main(String[] args) {
        SpringApplication.run(GrandPrixTrackerApplication.class, args);
    }
}
```

---

## Estructura del proyecto

```
src/
└── main/
    ├── java/
    │   └── com/uade/grandprixtracker/
    │       │
    │       ├── GrandPrixTrackerApplication.java   ← Punto de entrada
    │       │
    │       ├── event/                             ← Calendario: eventos, circuitos, ciudades, países
    │       ├── ticket/                            ← Entradas (tribunas) de cada evento
    │       ├── hotel/                             ← Hoteles y habitaciones
    │       ├── flight/                            ← Vuelos
    │       ├── payment/                           ← Métodos de pago y cobro (simulado)
    │       ├── booking/                           ← Checkout: CheckoutFacade + reservas
    │       ├── user/                              ← Clientes
    │       ├── auth/                              ← Seguridad: valida el JWT de Supabase Auth (SecurityConfig + respuestas 401/403)
    │       │
    │       ├── config/                            ← Configuración global (CORS, Clock, SOAP)
    │       └── shared/                            ← Excepciones y respuesta estándar de la API
    │
    └── resources/
        ├── application.properties                 ← Configuración de la app
        └── db/
            ├── auth_clientes_trigger.sql          ← Trigger que crea el cliente al registrarse (ya aplicado en Supabase)
            ├── indexes.sql                        ← Índices (ya aplicados en Supabase)
            ├── reservas_id_evento.sql             ← Columna reservas.id_evento + backfill
            └── seed.sql                           ← Datos de prueba (ya aplicados en Supabase)
```

Cada feature sigue la estructura `controller/`, `service/`, `repository/`, `model/` y `dto/` descripta abajo, con solo las carpetas que necesita.

---

## Correr en local

La app se conecta a la base de Supabase configurada en `application.properties`. Queda en `http://localhost:8080`.

```bash
./mvnw spring-boot:run
./mvnw test
```

El proyecto apunta a **Java 25**. Con JDK 21 se puede compilar y testear igual, pasando `-Dmaven.compiler.release=21`:

```bash
./mvnw spring-boot:run -Dmaven.compiler.release=21
./mvnw test -Dmaven.compiler.release=21
```

Si el IDE compiló antes con Java 25, Maven con JDK 21 falla con `class file version 69.0`. Se resuelve borrando `target/` (o con `./mvnw clean`).

---

## Arquitectura por feature

Cada feature agrupa **todo lo relacionado a esa funcionalidad** en una sola carpeta, en lugar de separar por capas técnicas. Esto hace que el código sea más fácil de navegar, mantener y escalar.

### Carpetas dentro de cada feature

| Carpeta | Responsabilidad |
|---|---|
| `controller/` | Define los endpoints REST. Recibe las requests HTTP y delega al service. |
| `service/` | Contiene la lógica de negocio. Es el núcleo de cada feature. |
| `repository/` | Interface que accede a la base de datos. Spring Data JPA genera las queries automáticamente. |
| `model/` | Clases que representan las tablas de la base de datos (entidades JPA anotadas con `@Entity`). |
| `dto/` | Objetos que entran y salen de la API. Separan la entidad interna de lo que se expone al cliente. |

### Ejemplo: feature `user`

```
user/
├── controller/
│   └── UserController.java       → @RestController, define GET /users, POST /users, etc.
├── service/
│   └── UserService.java          → lógica: crear usuario, validar datos, etc.
├── repository/
│   └── UserRepository.java       → interface JpaRepository<User, Long>
├── model/
│   └── User.java                 → @Entity, representa la tabla "users" en la BD
└── dto/
    ├── UserRequestDto.java        → datos que llegan del cliente al crear/editar
    └── UserResponseDto.java       → datos que se devuelven al cliente
```

---

## Carpetas globales

### `config/`

Configuración transversal a toda la aplicación. No pertenece a ninguna feature en particular.

```
config/
├── CorsConfig.java               → configuración de CORS
├── ClockConfig.java              → reloj inyectable (UTC)
└── SoapWebServiceConfig.java     → servicio SOAP en /ws
```

### `auth/`

Autenticación con **Supabase Auth**. El login y el registro los hace el front contra Supabase; el backend solo valida el token que llega en `Authorization: Bearer <token>` (Spring Security como OAuth2 Resource Server) y toma el cliente del claim `sub`.

```
auth/
├── config/
│   └── SecurityConfig.java              → qué rutas son públicas y cuáles exigen token; API stateless, sin CSRF
└── handler/
    ├── ApiAuthenticationEntryPoint.java → 401 con el envoltorio ApiResponse
    └── ApiAccessDeniedHandler.java      → 403 con el envoltorio ApiResponse
```

La verificación del token se configura en `application.properties` (`spring.security.oauth2.resourceserver.jwt.*`). Los valores por defecto apuntan al proyecto de Supabase y se pueden pisar con variables de entorno:

| Variable | Default |
|---|---|
| `SUPABASE_JWT_ISSUER_URI` | `https://zprznayvpeijjoiknird.supabase.co/auth/v1` |
| `SUPABASE_JWK_SET_URI` | `https://zprznayvpeijjoiknird.supabase.co/auth/v1/.well-known/jwks.json` |
| `SUPABASE_JWT_AUDIENCE` | `authenticated` |
| `SUPABASE_JWT_ALGORITHMS` | `ES256` (sin esto Spring asume RS256 y rechaza todos los tokens) |

No hace falta ningún secreto: los tokens se firman con una clave asimétrica y las claves públicas se leen del JWKS.

### `shared/`

Clases reutilizables que pueden ser usadas por cualquier feature.

```
shared/
├── exception/
│   ├── GlobalExceptionHandler.java   → maneja errores de forma centralizada
│   └── ResourceNotFoundException.java
└── response/
    └── ApiResponse.java              → estructura estándar de respuesta de la API
```

---

## Tecnologías

- **Java 25**
- **Spring Boot 4.1.1**
- **Maven 3.9.11**
- **PostgreSQL** (via Supabase)

---

## Documentación adicional

La carpeta [`docs/`](./docs/) contiene documentación técnica adicional:

- [`docs/api.md`](./docs/api.md) — Referencia de endpoints: qué recibe, qué devuelve y qué hace cada uno, cómo encadenarlos en el wizard de compra y datos de prueba. **Punto de partida para quien trabaje en el frontend.**
- [`docs/db.md`](./docs/db.md) — Esquema de la base de datos (Supabase / PostgreSQL).
- [`docs/checkout.md`](./docs/checkout.md) — Compra de paquetes: reglas de negocio, arquitectura y limitaciones.

A medida que crezca el proyecto, se van a ir sumando ahí documentos explicando la lógica de negocio de cada feature y decisiones de arquitectura relevantes.