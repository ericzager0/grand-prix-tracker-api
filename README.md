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
    │       ├── auth/                              ← Feature: autenticación
    │       │   ├── controller/
    │       │   ├── service/
    │       │   ├── repository/
    │       │   ├── model/
    │       │   └── dto/
    │       │
    │       ├── user/                              ← Feature: usuarios
    │       │   ├── controller/
    │       │   ├── service/
    │       │   ├── repository/
    │       │   ├── model/
    │       │   └── dto/
    │       │
    │       │   ... (más features a medida que crezca el proyecto)
    │       │
    │       ├── config/                            ← Configuración global
    │       └── shared/                            ← Clases compartidas entre features
    │
    └── resources/
        └── application.properties                 ← Configuración de la app
```

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
├── SecurityConfig.java           → configuración de Spring Security
├── CorsConfig.java               → configuración de CORS
└── ...
```

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