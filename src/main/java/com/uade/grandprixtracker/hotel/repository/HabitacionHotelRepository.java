package com.uade.grandprixtracker.hotel.repository;

import com.uade.grandprixtracker.hotel.model.HabitacionHotel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HabitacionHotelRepository extends JpaRepository<HabitacionHotel, UUID> {

    @Query("SELECT h FROM HabitacionHotel h JOIN FETCH h.hotel WHERE h.idHabitacion = :id")
    Optional<HabitacionHotel> findByIdWithHotel(@Param("id") UUID id);

    @Query("SELECT h FROM HabitacionHotel h JOIN FETCH h.hotel ho " +
           "WHERE ho.idCiudad = :idCiudad " +
           "ORDER BY ho.distanciaCircuitoKm ASC NULLS LAST, ho.nombre ASC, h.precioPorNocheUsd ASC")
    List<HabitacionHotel> findByCiudadWithHotel(@Param("idCiudad") UUID idCiudad);

    @Modifying
    @Query("UPDATE HabitacionHotel h SET h.stockDisponible = h.stockDisponible - 1 " +
           "WHERE h.idHabitacion = :id AND h.stockDisponible >= 1")
    int descontarStock(@Param("id") UUID id);
}
