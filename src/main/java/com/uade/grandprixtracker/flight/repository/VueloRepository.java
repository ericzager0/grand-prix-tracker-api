package com.uade.grandprixtracker.flight.repository;

import com.uade.grandprixtracker.flight.model.Vuelo;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VueloRepository extends JpaRepository<Vuelo, UUID> {

    @Query("SELECT v FROM Vuelo v JOIN FETCH v.origen JOIN FETCH v.destino WHERE v.idVuelo = :id")
    Optional<Vuelo> findByIdWithCiudades(@Param("id") UUID id);

    @Query("SELECT v FROM Vuelo v JOIN FETCH v.origen o JOIN FETCH v.destino d " +
           "WHERE (o.idCiudad = :idCiudad OR d.idCiudad = :idCiudad) AND v.fechaSalida > :desde " +
           "ORDER BY v.fechaSalida ASC")
    List<Vuelo> findDisponiblesPorCiudad(@Param("idCiudad") UUID idCiudad, @Param("desde") OffsetDateTime desde);

    @Modifying
    @Query("UPDATE Vuelo v SET v.stockAsientos = v.stockAsientos - :cantidad " +
           "WHERE v.idVuelo = :id AND v.stockAsientos >= :cantidad")
    int descontarStock(@Param("id") UUID id, @Param("cantidad") int cantidad);
}
