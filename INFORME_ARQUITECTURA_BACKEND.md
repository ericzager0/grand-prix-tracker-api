# Informe Técnico y Arquitectónico del Backend: Grand Prix Tracker

---

## 1. Patrones de Diseño Aplicados (Dónde y Cómo)

El backend de Grand Prix Tracker implementa patrones de diseño reconocidos por la industria (GoF y patrones empresariales de Martin Fowler), aplicados de forma idiomática con Spring Boot y Java 21+:

### A. Patrones Creacionales

#### 1. Inyección de Dependencias / Inversión de Control (Dependency Injection / IoC)
* **Dónde**: En todos los controladores, servicios, clientes y componentes del proyecto (`CheckoutFacade`, `TicketService`, `CompraConfirmadaEmailListener`, `UserController`, etc.).
* **Cómo**: Se utiliza inyección por constructor estricta con campos `private final`. No se recurre a `@Autowired` sobre campos privados, lo que garantiza inmutabilidad, previene ciclos de dependencias y permite instanciar cualquier clase en pruebas unitarias mediante mocks puros (`MockitoExtension`) sin levantar el contexto pesado de Spring.

#### 2. Factory / Client Builder
* **Dónde**: `TicketingMicroserviceClient.java`
* **Cómo**: Se encapsula la creación del cliente HTTP mediante `RestClient.builder()` configurando dinámicamente un `SimpleClientHttpRequestFactory` con políticas estrictas de `connectTimeout` y `readTimeout` inyectadas desde `application.properties`.

---

### B. Patrones Estructurales

#### 1. Patrón Facade (Fachada)
* **Dónde**: `CheckoutFacade.java`
* **Cómo**: El proceso de compra de un paquete es altamente complejo: requiere validar un evento, verificar disponibilidad y descontar stock de entradas en un sistema SOAP legado a través de un microservicio externo, reservar hotel, reservar vuelo, procesar el pago, aplicar cargos de transporte y persistir la reserva. `CheckoutFacade` centraliza toda esta orquestación en un único método `checkout()`, exponiendo una interfaz limpia hacia el `BookingController` y ocultando la complejidad de los múltiples servicios subyacentes.

#### 2. Patrón Adapter / Gateway (Adaptador de Servicios Externos)
* **Dónde**: 
  - `TicketingMicroserviceClient.java` (Integración HTTP/REST hacia el microservicio que interactúa con el SOAP legado de F1).
  - `BrevoEmailSender.java` (Gateway hacia la API REST de Brevo / Sendinblue).
* **Cómo**: Traducen llamadas y estructuras del dominio interno hacia los contratos y protocolos de servicios externos. Si el microservicio externo responde con error HTTP 409 (conflicto por stock en SOAP), el adaptador lo intercepta y lo convierte en una excepción de dominio tipada (`StockInsuficienteException`).

#### 3. Data Transfer Object (DTO)
* **Dónde**: En todos los subpaquetes `dto/` (`CheckoutRequestDto`, `ReservaResponseDto`, `UpdateUserProfileRequestDto`, `EntradaResponseDto`, etc.).
* **Cómo**: Implementados mediante **Java Records** inmutables. Desacoplan por completo el modelo de persistencia (entidades JPA / tablas de Supabase) de los contratos de entrada/salida de la API. Evitan sobrecarga de transferencia de datos (*over-fetching*), recursiones cíclicas en serialización JSON y vulnerabilidades de *Mass Assignment*.

#### 4. Mapper
* **Dónde**: `ReservaMapper.java`, `EventSoapMapper.java`.
* **Cómo**: Clases utilitarias puras que transforman entidades del dominio JPA a sus respectivos DTOs (REST o SOAP XML). Mantienen a las entidades libres de lógica de serialización y a los controladores limpios de conversiones manuales.

#### 5. Filter / Interceptor (Chain of Responsibility)
* **Dónde**: `SecurityConfig.java` y la cadena de filtros de Spring Security.
* **Cómo**: Las solicitudes entrantes atraviesan filtros antes de alcanzar los controladores. Si una ruta protegida recibe una petición, el filtro valida el token JWT emitido por Supabase, extrae el `sub` (User ID) y establece el `SecurityContextHolder`.

---

### C. Patrones de Comportamiento

#### 1. Observer / Arquitectura Orientada a Eventos (Domain Events)
* **Dónde**: 
  - Emisor: `CheckoutFacade.java` (`eventPublisher.publishEvent(new CompraConfirmadaEvent(...))`).
  - Suscriptores: `CompraConfirmadaEmailListener.java` y `NotificationEventListener.java`.
* **Cómo**: Desacopla la lógica central de negocio de las acciones secundarias. `CheckoutFacade` no conoce ni interactúa con el servicio de emails; solo publica el evento `CompraConfirmadaEvent`. El listener escucha este evento con:
  - `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`: Solo se dispara si la transacción del checkout se confirmó exitosamente en la base de datos.
  - `@Async`: Se procesa en un hilo de fondo, permitiendo que el cliente reciba la respuesta HTTP 201 en milisegundos sin esperar el envío de emails.

#### 2. Strategy (Estrategia)
* **Dónde**:
  - `NotificationContentStrategy.java` con sus implementaciones:
    - `BookingNotificationStrategy`
    - `OfferNotificationStrategy`
    - `PurchaseStatusNotificationStrategy`
  - `NotificationChannel.java` con `EmailNotificationChannel` e `InAppNotificationChannel`.
* **Cómo**: Permite definir una familia de algoritmos para procesar notificaciones. Cada tipo de evento sabe cómo formatear su mensaje y cada canal sabe cómo entregar la notificación (in-app o correo), seleccionándose dinámicamente en tiempo de ejecución.

#### 3. Dispatcher / Mediator
* **Dónde**: `NotificationDispatcher.java`
* **Cómo**: Actúa como despachador central que recibe una notificación abstracta y la distribuye hacia los canales suscritos según el `DeliveryMode` (`EMAIL`, `IN_APP` o `BOTH`).

#### 4. Delegation / Handler
* **Dónde**: `UserHandler.java` y `UserController.java`
* **Cómo**: El controlador HTTP delega la extracción de claims/identidad del token JWT al `UserHandler`, el cual valida el contexto de seguridad y luego invoca al `UserService`. Esto aísla al controlador de los detalles de infraestructura de autenticación.

#### 5. Aspect-Oriented Programming (AOP) e Interceptores
* **Dónde**: 
  - Transacciones declarativas con `@Transactional`.
  - `GlobalExceptionHandler.java` con `@RestControllerAdvice`.
* **Cómo**: Centraliza el control de transacciones ACID y la captura de errores en toda la aplicación. Cualquier excepción (`ResourceNotFoundException`, `StockInsuficienteException`, `MethodArgumentNotValidException`) es interceptada y formateada en una respuesta estandarizada `ApiResponse<T>`.

#### 6. Repositorio (Repository Pattern)
* **Dónde**: `ReservaRepository`, `ClienteRepository`, `EventoF1Repository`, `EntradaGradaRepository`, `MetodoPagoRepository`.
* **Cómo**: Mediante Spring Data JPA, se desacopla la lógica de negocio de las consultas SQL nativas, proporcionando operaciones CRUD tipadas, paginación y métodos derivados automáticos contra la base de datos Supabase/PostgreSQL.

---

## 2. Ventajas de la Separación Modular por Dominio (Package-by-Feature)

El backend no está dividido en capas horizontales genéricas (todos los controllers juntos, todos los services juntos), sino por **componentes verticales de dominio** (`booking`, `ticket`, `hotel`, `flight`, `event`, `payment`, `user`, `notification`, `auth`, `shared`), donde cada uno contiene sus propios DTOs, Controllers, Services y Repositories.

```
com.uade.grandprixtracker
├── auth/           -> Seguridad, JWT, configuración de acceso
├── booking/        -> Checkout, reservas, compras, transacciones
├── event/          -> Circuitos, grandes premios, endpoints REST y SOAP
├── flight/         -> Vuelos, aerolíneas, disponibilidad
├── hotel/          -> Hoteles, habitaciones, estadías
├── notification/   -> Estrategias, canales, listeners, plantillas de correo
├── payment/        -> Métodos de pago, procesamiento de cobros
├── shared/         -> Excepciones globales, respuestas tipadas, constantes
├── ticket/         -> Entradas de tribuna, cliente HTTP del microservicio SOAP
└── user/           -> Perfil del cliente, sincronización con Supabase
```

### Ventajas Clave de este Enfoque:

1. **Alta Cohesión y Bajo Acoplamiento (*High Cohesion, Low Coupling*)**:
   - Cada paquete agrupa clases que cambian por las mismas razones de negocio. Un cambio en la estructura de un método de pago (`payment`) no afecta al módulo de vuelos (`flight`) ni al de eventos (`event`).

2. **Aislamiento de Contratos (DTOs Específicos por Dominio)**:
   - Tener DTOs exclusivos para cada componente evita la contaminación de datos. Por ejemplo, `UpdateUserProfileRequestDto` solo contiene los campos modificables del perfil (`nombre`, `apellido`, `telefono`, `dni`), impidiendo que un usuario modifique campos sensibles como su `email` o `id`.

3. **Facilidad de Migración hacia Microservicios**:
   - La arquitectura sigue el principio de **Monolito Modular**. Si mañana el módulo de `ticket` o `booking` necesita desplegarse como un microservicio independiente por demanda de tráfico, ya posee sus propios controladores, servicios, repositorios y contratos listos para ser extraídos con mínimo esfuerzo.

4. **Principio de Responsabilidad Única (SRP - SOLID)**:
   - **Controller**: Solo valida el protocolo HTTP, códigos de estado y ruteo.
   - **Service**: Implementa reglas de negocio, validaciones lógicas y transacciones.
   - **Repository**: Solo gestiona la persistencia y lectura de datos.
   - **DTO**: Solo define la estructura de transporte de datos.

5. **Testeabilidad Aislada y Rápida**:
   - Al no existir dependencias cruzadas desordenadas, cada componente se prueba de manera unitaria con `@ExtendWith(MockitoExtension.class)`. Las pruebas se ejecutan en segundos sin necesidad de iniciar una base de datos real ni un servidor embebido (los 131 tests del backend se completan en ~12 segundos).

---

## 3. Mecanismos y Funcionamientos Clave del Backend

### A. Proceso de Checkout Atómico y Prevención de Deadlocks
El método `checkout()` en `CheckoutFacade` es el núcleo transaccional del sistema:
1. **Transaccionalidad ACID**: Anotado con `@Transactional`, asegura que si falla cualquier paso (por ejemplo, stock insuficiente o error en pasarela de pagos), se ejecuta un *rollback* completo y la base de datos queda intacta.
2. **Prevención de Bloqueos Mutuos (Deadlocks Concurrentes)**:
   Al reservar entradas, habitaciones y vuelos, los ítems se ordenan determinísticamente por su ID (`Comparator.comparing(...)`):
   ```java
   request.entradas().stream()
       .sorted(Comparator.comparing(EntradaItem::idEntrada))
       ...
   ```
   Esto garantiza que si dos clientes intentan comprar las mismas entradas simultáneamente, los bloqueos en base de datos se adquieren siempre en el mismo orden, eliminando la posibilidad de *deadlocks* cíclicos.
3. **Cálculo de Adicionales de Negocio**:
   Si el cliente selecciona `incluyeTransporte = true`, se agrega el cargo fijo de **US$ 30.00** a la reserva y se añade la línea correspondiente tanto en la base de datos como en el resumen de compra.

---

### B. Integración Híbrida: REST, Microservicios y SOAP Legado
El backend conecta sistemas de diferentes generaciones tecnológicas:
* **Microservicio de Ticketing (`TicketingMicroserviceClient`)**:
  - La compra de entradas requiere validar y descontar cupos en un sistema legado SOAP de F1.
  - El backend se comunica vía HTTP/REST con el microservicio intermediario de ticketing.
  - Se implementan timeouts de conexión (15s) y lectura (30s) para evitar que hilos del backend queden colgados si el servicio externo demora.
  - Los errores HTTP 409 (*Conflict*) son interpretados y convertidos a `StockInsuficienteException` para informar con precisión al usuario.
* **Exposición Dual de Eventos (REST y SOAP)**:
  - El sistema expone endpoints REST (`/events`) para la SPA del frontend y simultáneamente endpoints SOAP mediante Spring WS (`EventSoapEndpoint`), respondiendo a esquemas XML (XSD) para sistemas que consumen servicios web empresariales tradicionales.

---

### C. Sistema de Notificaciones Asíncrono y Ultra-Resiliente
El envío de correos electrónicos tras una compra exitosa implementa una política de **falla segura**:
* **Garantía AFTER_COMMIT**: La compra nunca falla porque el servicio de correos (Brevo) esté caído o sin saldo. El evento se procesa únicamente cuando el checkout ya está persistido en PostgreSQL.
* **Ejecución Asíncrona (`@Async`)**: El usuario recibe su confirmación de compra de inmediato; el renderizado HTML y la llamada a la API externa de Brevo ocurren en un hilo separado del pool de tareas.
* **Optimización de Plantilla HTML para Gmail**:
  - Las imágenes embebidas en Base64 exceden el límite de 102 KB de Gmail, provocando que el correo se corte (`[Mensaje recortado]`).
  - La plantilla `compra-confirmada.html` pesa menos de **6 KB**, sirviendo los recursos multimedia mediante URLs HTTPS públicas optimizadas.
  - Incluye barra inferior estilizada (`#0B0B10`), cabecera en rojo corporativo (`#E10600`) y visualización condicional de la línea de **Translado (US$ 30.00)**.

---

### D. Seguridad y Autenticación con Supabase
* **Autenticación Delegada (Stateless)**:
  - El frontend autentica al usuario contra Supabase Auth y envía un token Bearer JWT en los headers de cada petición.
  - El backend valida el token, extrayendo el identificador único del usuario (`sub`).
* **Sincronización de Datos del Usuario**:
  - Los datos de perfil (`nombre`, `apellido`, `telefono`, `dni`) y los métodos de pago se gestionan mediante endpoints dedicados (`PUT /users/profile`, `POST /payment-methods`, `PUT /payment-methods/{id}`) con persistencia directa en las tablas relacionales de Supabase, asegurando consistencia entre la cuenta de autenticación y la ficha de cliente.

---

### E. Estandarización de Respuestas y Manejo Global de Excepciones
Todas las respuestas de la aplicación siguen un estándar uniforme:
```json
{
  "success": true,
  "data": { ... },
  "message": "Operación exitosa"
}
```
En caso de error, el `GlobalExceptionHandler` intercepta las excepciones y genera códigos de estado HTTP semánticos (400, 404, 409, 500, etc.) con mensajes legibles para el frontend, evitando la exposición de stacktraces internos o información sensible de la infraestructura.
