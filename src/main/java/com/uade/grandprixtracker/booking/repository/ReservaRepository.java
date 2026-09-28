package com.uade.grandprixtracker.booking.repository;

import com.uade.grandprixtracker.booking.model.Reserva;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservaRepository extends JpaRepository<Reserva, UUID> {
}
