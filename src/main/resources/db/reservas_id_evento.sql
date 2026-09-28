-- ====================================================================
-- Grand Prix Tracker - reservas.id_evento (ya aplicado en Supabase el 2026-09-28)
-- Ejecutar en: Supabase Dashboard -> SQL Editor
-- Objetivo: que cada reserva sepa de qué Gran Premio es (GET /bookings).
-- Cambio aditivo: columna nullable sin default, los INSERT existentes siguen funcionando.
-- ====================================================================

BEGIN;

ALTER TABLE public.reservas
    ADD COLUMN IF NOT EXISTS id_evento uuid NULL
    REFERENCES public.eventos_f1 (id_evento);

CREATE INDEX IF NOT EXISTS idx_reservas_id_evento
    ON public.reservas (id_evento);

-- Backfill de las reservas anteriores. Solo completa filas en null y solo si hay un único
-- evento candidato; lo que no se pueda resolver (por ejemplo, reservas de solo vuelos) queda en null.

-- 1) Por entradas: exacto, cada entrada pertenece a un evento.
UPDATE public.reservas r SET id_evento = x.id_evento
FROM (
    SELECT d.id_reserva, min(e.id_evento::text)::uuid AS id_evento
    FROM public.reserva_detalle_entradas d
    JOIN public.entradas_gradas e ON e.id_entrada = d.id_entrada
    GROUP BY d.id_reserva
    HAVING count(DISTINCT e.id_evento) = 1
) x
WHERE r.id_reserva = x.id_reserva AND r.id_evento IS NULL;

-- 2) Por hotel: ciudad del hotel = ciudad del circuito y check-in = fecha_inicio - 1 (regla de negocio).
UPDATE public.reservas r SET id_evento = x.id_evento
FROM (
    SELECT d.id_reserva, min(ev.id_evento::text)::uuid AS id_evento
    FROM public.reserva_detalle_hoteles d
    JOIN public.habitaciones_hotel h ON h.id_habitacion = d.id_habitacion
    JOIN public.hoteles ho           ON ho.id_hotel = h.id_hotel
    JOIN public.circuitos c          ON c.id_ciudad = ho.id_ciudad
    JOIN public.eventos_f1 ev        ON ev.id_circuito = c.id_circuito
                                    AND d.fecha_check_in = ev.fecha_inicio - 1
    GROUP BY d.id_reserva
    HAVING count(DISTINCT ev.id_evento) = 1
) x
WHERE r.id_reserva = x.id_reserva AND r.id_evento IS NULL;

COMMIT;
