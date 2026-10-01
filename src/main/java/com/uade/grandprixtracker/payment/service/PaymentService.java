package com.uade.grandprixtracker.payment.service;

import com.uade.grandprixtracker.payment.dto.MetodoPagoResponseDto;
import com.uade.grandprixtracker.payment.dto.PagoRequestDto;
import com.uade.grandprixtracker.payment.model.MetodoPago;
import com.uade.grandprixtracker.payment.repository.MetodoPagoRepository;
import com.uade.grandprixtracker.shared.exception.ResourceNotFoundException;
import com.uade.grandprixtracker.user.model.Cliente;
import com.uade.grandprixtracker.user.repository.ClienteRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private static final DateTimeFormatter EXPIRACION = DateTimeFormatter.ofPattern("MM/yy");

    private final MetodoPagoRepository metodoPagoRepository;
    private final ClienteRepository clienteRepository;
    private final Clock clock;

    public PaymentService(MetodoPagoRepository metodoPagoRepository, ClienteRepository clienteRepository, Clock clock) {
        this.metodoPagoRepository = metodoPagoRepository;
        this.clienteRepository = clienteRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<MetodoPagoResponseDto> listarPorCliente(UUID idCliente) {
        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "id", idCliente));

        return metodoPagoRepository.findByClienteIdClienteOrderByCreatedAtDesc(idCliente)
                .stream()
                .map(m -> new MetodoPagoResponseDto(
                        m.getIdMetodo(),
                        m.getTipo(),
                        m.getUltimos4Digitos(),
                        m.getFechaExpiracion(),
                        estaVencida(m.getFechaExpiracion()),
                        m.getNombreTitular(),
                        cliente.getTelefono(),
                        cliente.getDni()))
                .toList();
    }

    /**
     * Pago simulado: no hay pasarela real, el cobro se considera aprobado si el método de pago es válido.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public MetodoPago cobrar(Cliente cliente, PagoRequestDto pago) {
        if (pago.idMetodoPago() != null) {
            MetodoPago guardado = metodoPagoRepository.findByIdMetodoAndClienteIdCliente(pago.idMetodoPago(), cliente.getIdCliente())
                    .orElseThrow(() -> new ResourceNotFoundException("Método de pago", "id", pago.idMetodoPago()));
            if (estaVencida(guardado.getFechaExpiracion())) {
                throw new IllegalArgumentException("La tarjeta está vencida");
            }
            return guardado;
        }

        if (isBlank(pago.tipo()) || isBlank(pago.ultimos4Digitos())
                || isBlank(pago.fechaExpiracion()) || isBlank(pago.proveedorToken())) {
            throw new IllegalArgumentException("Se debe indicar idMetodoPago o los datos completos de una tarjeta nueva");
        }

        if (estaVencida(pago.fechaExpiracion())) {
            throw new IllegalArgumentException("La tarjeta está vencida");
        }

        return metodoPagoRepository.save(new MetodoPago(
                null,
                cliente,
                pago.tipo(),
                pago.ultimos4Digitos(),
                pago.proveedorToken(),
                pago.fechaExpiracion(),
                OffsetDateTime.now(clock),
                pago.nombreTitular()
        ));
    }

    private boolean estaVencida(String fechaExpiracion) {
        return YearMonth.parse(fechaExpiracion, EXPIRACION).isBefore(YearMonth.now(clock));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
