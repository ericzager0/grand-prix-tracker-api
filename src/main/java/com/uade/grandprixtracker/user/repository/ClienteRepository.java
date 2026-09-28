package com.uade.grandprixtracker.user.repository;

import com.uade.grandprixtracker.user.model.Cliente;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {
}
