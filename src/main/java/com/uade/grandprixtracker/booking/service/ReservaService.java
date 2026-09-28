package com.uade.grandprixtracker.booking.service;

import com.uade.grandprixtracker.booking.dto.ReservaResponseDto;
import com.uade.grandprixtracker.booking.model.Reserva;
import com.uade.grandprixtracker.booking.repository.ReservaRepository;
import com.uade.grandprixtracker.shared.exception.ResourceNotFoundException;
import com.uade.grandprixtracker.user.repository.ClienteRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta de las reservas de un cliente (sección "Reservas" del perfil).
 */
@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final ClienteRepository clienteRepository;

    public ReservaService(ReservaRepository reservaRepository, ClienteRepository clienteRepository) {
        this.reservaRepository = reservaRepository;
        this.clienteRepository = clienteRepository;
    }

    @Transactional(readOnly = true)
    public List<ReservaResponseDto> listarPorCliente(UUID idCliente) {
        validarCliente(idCliente);

        List<Reserva> reservas = reservaRepository.findByClienteConEvento(idCliente);
        cargarDetalles(reservas);
        return reservas.stream().map(ReservaMapper::toDto).toList();
    }

    /**
     * Una reserva de otro cliente se responde igual que una inexistente, para no revelar que existe.
     */
    @Transactional(readOnly = true)
    public ReservaResponseDto obtener(UUID idCliente, UUID idReserva) {
        validarCliente(idCliente);

        Reserva reserva = reservaRepository.findByIdAndClienteConEvento(idReserva, idCliente)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));
        cargarDetalles(List.of(reserva));
        return ReservaMapper.toDto(reserva);
    }

    private void validarCliente(UUID idCliente) {
        if (!clienteRepository.existsById(idCliente)) {
            throw new ResourceNotFoundException("Cliente no encontrado");
        }
    }

    // Inicializa las colecciones de las reservas ya cargadas en el contexto de persistencia.
    private void cargarDetalles(List<Reserva> reservas) {
        if (reservas.isEmpty()) {
            return;
        }
        reservaRepository.fetchEntradas(reservas);
        reservaRepository.fetchHabitaciones(reservas);
        reservaRepository.fetchVuelos(reservas);
    }
}
