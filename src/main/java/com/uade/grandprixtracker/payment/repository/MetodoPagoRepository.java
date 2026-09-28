package com.uade.grandprixtracker.payment.repository;

import com.uade.grandprixtracker.payment.model.MetodoPago;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MetodoPagoRepository extends JpaRepository<MetodoPago, UUID> {

    Optional<MetodoPago> findByIdMetodoAndClienteIdCliente(UUID idMetodo, UUID idCliente);

    List<MetodoPago> findByClienteIdClienteOrderByCreatedAtDesc(UUID idCliente);
}
