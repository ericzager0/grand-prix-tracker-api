package com.uade.grandprixtracker.ticket.repository;

import com.uade.grandprixtracker.ticket.model.EntradaGrada;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EntradaGradaRepository extends JpaRepository<EntradaGrada, UUID> {

    List<EntradaGrada> findByIdEventoOrderByPrecioUsdAsc(UUID idEvento);

    @Modifying
    @Query("UPDATE EntradaGrada e SET e.stockDisponible = e.stockDisponible - :cantidad " +
           "WHERE e.idEntrada = :id AND e.stockDisponible >= :cantidad")
    int descontarStock(@Param("id") UUID id, @Param("cantidad") int cantidad);
}
