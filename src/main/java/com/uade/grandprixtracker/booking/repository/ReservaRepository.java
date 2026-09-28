package com.uade.grandprixtracker.booking.repository;

import com.uade.grandprixtracker.booking.model.Reserva;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Las colecciones de detalle son listas y Hibernate no permite traer más de una por query con fetch join,
 * así que la lectura se hace en una query para la cabecera y una por cada colección, sobre esas reservas
 * (siempre 4 queries, sin importar cuántas reservas haya).
 */
public interface ReservaRepository extends JpaRepository<Reserva, UUID> {

    @Query("SELECT r FROM Reserva r " +
           "LEFT JOIN FETCH r.metodoPago " +
           "LEFT JOIN FETCH r.evento e " +
           "LEFT JOIN FETCH e.circuito c " +
           "LEFT JOIN FETCH c.ciudad ci " +
           "LEFT JOIN FETCH ci.pais " +
           "WHERE r.cliente.idCliente = :idCliente " +
           "ORDER BY r.fechaCompra DESC NULLS LAST, r.idReserva")
    List<Reserva> findByClienteConEvento(@Param("idCliente") UUID idCliente);

    @Query("SELECT r FROM Reserva r " +
           "LEFT JOIN FETCH r.metodoPago " +
           "LEFT JOIN FETCH r.evento e " +
           "LEFT JOIN FETCH e.circuito c " +
           "LEFT JOIN FETCH c.ciudad ci " +
           "LEFT JOIN FETCH ci.pais " +
           "WHERE r.idReserva = :idReserva AND r.cliente.idCliente = :idCliente")
    Optional<Reserva> findByIdAndClienteConEvento(@Param("idReserva") UUID idReserva, @Param("idCliente") UUID idCliente);

    @Query("SELECT r FROM Reserva r " +
           "LEFT JOIN FETCH r.entradas d " +
           "LEFT JOIN FETCH d.entrada " +
           "WHERE r IN :reservas")
    List<Reserva> fetchEntradas(@Param("reservas") List<Reserva> reservas);

    @Query("SELECT r FROM Reserva r " +
           "LEFT JOIN FETCH r.habitaciones d " +
           "LEFT JOIN FETCH d.habitacion h " +
           "LEFT JOIN FETCH h.hotel " +
           "WHERE r IN :reservas")
    List<Reserva> fetchHabitaciones(@Param("reservas") List<Reserva> reservas);

    @Query("SELECT r FROM Reserva r " +
           "LEFT JOIN FETCH r.vuelos d " +
           "LEFT JOIN FETCH d.vuelo v " +
           "LEFT JOIN FETCH v.origen " +
           "LEFT JOIN FETCH v.destino " +
           "WHERE r IN :reservas")
    List<Reserva> fetchVuelos(@Param("reservas") List<Reserva> reservas);
}
