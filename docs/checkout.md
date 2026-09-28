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
   - La reserva guarda el evento (`reservas.id_evento`), para que `GET /bookings` pueda mostrar de qué Gran Premio es.
   - No se puede comprar para un evento en estado `Finalizado` **ni para uno cuya `fecha_fin` ya pasó**, aunque figure como `Proximo`. La columna `estado` se carga a mano y puede quedar desactualizada; las fechas son la fuente confiable.
4. **Stock atómico.** El stock se descuenta con `UPDATE ... SET stock = stock - n WHERE id = ? AND stock >= n`. Si la condición no se cumple (otro usuario compró antes), se responde `409`. No hay lecturas previas de stock que puedan quedar desactualizadas.
5. **Orden de descuento.** Dentro de cada tipo de producto, los ítems se procesan ordenados por id, para que dos compras concurrentes sobre los mismos ítems no se bloqueen mutuamente (deadlock).
6. **Pago simulado.** No hay pasarela de pago real: el cobro se considera aprobado si el método de pago es válido y la tarjeta no está vencida, tanto si es una tarjeta guardada como si es nueva. La reserva se crea directamente en estado `Pagada`. Una tarjeta nueva queda guardada en `metodos_pago` del cliente, porque la reserva necesita referenciarla.
7. **Código de confirmación.** Formato `GP-XXXXXXXX` (8 caracteres, sin `0/O/1/I` para evitar confusiones al leerlo). La columna es `UNIQUE` en la base.

## Arquitectura

`BookingController` → `CheckoutFacade` → `TicketService`, `HotelService`, `FlightService`, `PaymentService`.

Las consultas de reservas (`GET /bookings` y `GET /bookings/{idReserva}`) van por `ReservaService`, fuera del facade porque no compran nada. El checkout y las consultas arman la respuesta con el mismo `ReservaMapper`, así que las tres devuelven la misma forma.

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
| `booking` | `GET /bookings`, `GET /bookings/{idReserva}` (`ReservaService`) | `POST /bookings` → `CheckoutFacade.checkout` |

Las fechas y horas se toman de un `Clock` inyectado (`ClockConfig`, en UTC), para que las validaciones que dependen de "hoy" se puedan testear con una fecha fija.

## Limitaciones conocidas

- **Stock de hotel sin fechas.** `habitaciones_hotel.stock_disponible` es un contador único, no por noche: reservar una habitación la descuenta para todo el evento, sin importar las fechas. Si se necesita disponibilidad por fecha, hace falta otro modelo de datos.
- **Regla de fechas de hotel no exigida por el backend.** Por negocio, el check-in es 1 día antes de `fecha_inicio` del evento y el check-out 1 día después de `fecha_fin` (carrera de jueves a domingo → miércoles a lunes). Hoy la aplica el front; el backend solo valida que check-out sea posterior a check-in y que check-in no sea en el pasado. Para exigirla, el checkout debería rechazar otras fechas o directamente calcularlas él y dejar de recibirlas en el body.
- **Precio unitario no guardado.** Las tablas `reserva_detalle_*` guardan el subtotal pero no el precio unitario. La respuesta lo calcula como `subtotal / cantidad` (o `/ noches`, `/ pasajeros`), que da exactamente el precio pagado porque el subtotal se guardó como `precio × cantidad`.
- **Reservas viejas sin evento.** Las reservas anteriores a `reservas.id_evento` se completaron con un backfill; las que no se pudieron resolver (por ejemplo, de solo vuelos) devuelven `evento: null`. Ver [`db.md`](./db.md#reservas).
- **Traslados.** `reservas.incluye_transporte` existe en la base pero no hay tabla de traslados; hoy siempre queda en `false`.
- **Cancelaciones.** No hay endpoint para cancelar una reserva ni para devolver stock.
- **Estado de los eventos.** `eventos_f1.estado` no se actualiza solo. El checkout se protege usando `fecha_fin`, pero el calendario sigue mostrando el estado que figura en la base.
- **Stock de hotel compartido con el microservicio.** Hoteles y habitaciones los carga un microservicio del equipo, y el checkout descuenta `habitaciones_hotel.stock_disponible` en cada compra. Si el microservicio vuelve a escribir el stock al sincronizar, pisa esos descuentos y se pueden vender habitaciones que ya no hay. Además, una habitación con reservas no se puede borrar (FK `ON DELETE RESTRICT`), así que el microservicio tiene que actualizar por id en vez de borrar y recargar.
- **Identificación del cliente.** Hasta integrar Supabase Auth, el cliente llega en el header `X-Cliente-Id` y no se verifica: cualquiera que conozca un id puede comprar a su nombre o ver sus tarjetas. Es aceptable para desarrollo, no para producción.

## Limpiar compras de prueba

Para deshacer una compra (devolver el stock y borrar la reserva), correr en Supabase → SQL Editor, reemplazando el código de confirmación:

```sql
BEGIN;

CREATE TEMP TABLE reserva_a_borrar ON COMMIT DROP AS
SELECT id_reserva FROM reservas WHERE codigo_confirmacion = 'GP-XXXXXXXX';

UPDATE entradas_gradas e SET stock_disponible = e.stock_disponible + d.cantidad
FROM reserva_detalle_entradas d
WHERE d.id_entrada = e.id_entrada AND d.id_reserva IN (SELECT id_reserva FROM reserva_a_borrar);

UPDATE habitaciones_hotel h SET stock_disponible = h.stock_disponible + 1
FROM reserva_detalle_hoteles d
WHERE d.id_habitacion = h.id_habitacion AND d.id_reserva IN (SELECT id_reserva FROM reserva_a_borrar);

UPDATE vuelos v SET stock_asientos = v.stock_asientos + d.cantidad_pasajeros
FROM reserva_detalle_vuelos d
WHERE d.id_vuelo = v.id_vuelo AND d.id_reserva IN (SELECT id_reserva FROM reserva_a_borrar);

-- Los detalles se borran en cascada (FK ON DELETE CASCADE).
DELETE FROM reservas WHERE id_reserva IN (SELECT id_reserva FROM reserva_a_borrar);

COMMIT;
```

Si la compra se pagó con una tarjeta nueva, esa tarjeta queda guardada en `metodos_pago`; borrarla aparte si no se quiere conservar.
