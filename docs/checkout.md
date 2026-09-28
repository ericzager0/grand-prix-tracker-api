# Checkout de paquetes

Compra de un paquete (entradas + hotel + vuelos) para un Gran Premio. El wizard del frontend arma el paquete en memoria y, al confirmar, hace **una sola llamada** al backend.

El contrato del endpoint (`POST /bookings`: headers, body, respuesta y errores) está en [`api.md`](./api.md#post-bookings). Este documento explica las reglas de negocio y las decisiones detrás.

## Reglas de negocio

1. **Todo o nada.** El checkout corre en una única transacción: si falla cualquier paso (stock, validación, pago), no se descuenta stock de nada, no se guarda la tarjeta nueva y no se crea la reserva.
2. **Precios del lado del servidor.** Los subtotales se calculan con los precios actuales de la base:
   - Entradas: `precio_usd × cantidad`
   - Hotel: `precio_por_noche_usd × noches` (noches = días entre check-in y check-out)
   - Vuelos: `precio_usd × cantidad_pasajeros`
   - `total_usd` = suma de todos los subtotales.
3. **Coherencia con el evento.**
   - Las entradas tienen que pertenecer al evento elegido.
   - Los hoteles tienen que estar en la ciudad del circuito del evento.
   - Los vuelos tienen que llegar a **o** salir de la ciudad del circuito (ida o vuelta) y todavía no haber partido.
   - No se puede comprar para un evento en estado `Finalizado` **ni para uno cuya `fecha_fin` ya pasó**, aunque figure como `Proximo`. La columna `estado` se carga a mano y puede quedar desactualizada; las fechas son la fuente confiable.
4. **Stock atómico.** El stock se descuenta con `UPDATE ... SET stock = stock - n WHERE id = ? AND stock >= n`. Si la condición no se cumple (otro usuario compró antes), se responde `409`. No hay lecturas previas de stock que puedan quedar desactualizadas.
5. **Orden de descuento.** Dentro de cada tipo de producto, los ítems se procesan ordenados por id, para que dos compras concurrentes sobre los mismos ítems no se bloqueen mutuamente (deadlock).
6. **Pago simulado.** No hay pasarela de pago real: el cobro se considera aprobado si el método de pago es válido y la tarjeta no está vencida, tanto si es una tarjeta guardada como si es nueva. La reserva se crea directamente en estado `Pagada`. Una tarjeta nueva queda guardada en `metodos_pago` del cliente, porque la reserva necesita referenciarla.
7. **Código de confirmación.** Formato `GP-XXXXXXXX` (8 caracteres, sin `0/O/1/I` para evitar confusiones al leerlo). La columna es `UNIQUE` en la base.

## Arquitectura

`BookingController` → `CheckoutFacade` → `TicketService`, `HotelService`, `FlightService`, `PaymentService`.

El `CheckoutFacade` (patrón Facade) es el único punto de entrada del checkout y coordina a los servicios de cada producto. Hoy cada servicio trabaja contra las tablas de Supabase; cuando se integren los sistemas externos (hoteles vía SOAP, ticketera vía REST), se cambia la implementación de cada servicio sin tocar el endpoint ni el facade.

Cada servicio de producto tiene dos tipos de operación:
- **Listado** (`listarPorEvento`, `listarPorCliente`): lo usan los controllers de cada feature (`TicketController`, `HotelController`, `FlightController`, `PaymentController`) para los pasos del wizard. Transacción de solo lectura.
- **Reserva / cobro** (`reservar`, `cobrar`): exigen una transacción ya abierta (`Propagation.MANDATORY`), así que solo se pueden usar desde el facade y siempre quedan dentro de la transacción única del checkout.

| Feature (paquete) | Endpoint de lectura | Operación de checkout |
|---|---|---|
| `ticket` | `GET /events/{id}/tickets` | `TicketService.reservar` |
| `hotel` | `GET /events/{id}/hotels` | `HotelService.reservar` |
| `flight` | `GET /events/{id}/flights` | `FlightService.reservar` |
| `payment` | `GET /payment-methods` | `PaymentService.cobrar` |
| `booking` | — | `POST /bookings` → `CheckoutFacade.checkout` |

Las fechas y horas se toman de un `Clock` inyectado (`ClockConfig`, en UTC), para que las validaciones que dependen de "hoy" se puedan testear con una fecha fija.

## Limitaciones conocidas

- **Stock de hotel sin fechas.** `habitaciones_hotel.stock_disponible` es un contador único, no por noche: reservar una habitación la descuenta para todo el evento, sin importar las fechas. Si se necesita disponibilidad por fecha, hace falta otro modelo de datos.
- **Fechas de hotel no atadas al evento.** Se valida que check-out sea posterior a check-in y que check-in no sea en el pasado, pero no que la estadía coincida con el fin de semana de la carrera.
- **Traslados.** `reservas.incluye_transporte` existe en la base pero no hay tabla de traslados; hoy siempre queda en `false`.
- **Cancelaciones.** No hay endpoint para cancelar una reserva ni para devolver stock.
- **Estado de los eventos.** `eventos_f1.estado` no se actualiza solo. El checkout se protege usando `fecha_fin`, pero el calendario sigue mostrando el estado que figura en la base.
- **Identificación del cliente.** Hasta integrar Supabase Auth, el cliente llega en el header `X-Cliente-Id` y no se verifica: cualquiera que conozca un id puede comprar a su nombre o ver sus tarjetas. Es aceptable para desarrollo, no para producción.
