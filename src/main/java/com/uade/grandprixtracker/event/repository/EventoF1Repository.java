package com.uade.grandprixtracker.event.repository;

import com.uade.grandprixtracker.event.model.EventoF1;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventoF1Repository extends JpaRepository<EventoF1, UUID> {

    @Query("SELECT e FROM EventoF1 e " +
           "JOIN FETCH e.circuito c " +
           "JOIN FETCH c.ciudad ci " +
           "JOIN FETCH ci.pais p " +
           "ORDER BY e.fechaInicio ASC")
    List<EventoF1> findAllWithCircuitCityCountry();

    @Query("SELECT e FROM EventoF1 e " +
           "JOIN FETCH e.circuito c " +
           "JOIN FETCH c.ciudad ci " +
           "JOIN FETCH ci.pais p " +
           "WHERE e.idEvento = :id")
    Optional<EventoF1> findByIdWithCircuitCityCountry(@Param("id") UUID id);
}

