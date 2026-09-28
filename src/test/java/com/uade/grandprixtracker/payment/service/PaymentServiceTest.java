package com.uade.grandprixtracker.payment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.uade.grandprixtracker.payment.dto.MetodoPagoResponseDto;
import com.uade.grandprixtracker.payment.dto.PagoRequestDto;
import com.uade.grandprixtracker.payment.model.MetodoPago;
import com.uade.grandprixtracker.payment.repository.MetodoPagoRepository;
import com.uade.grandprixtracker.shared.exception.ResourceNotFoundException;
import com.uade.grandprixtracker.user.model.Cliente;
import com.uade.grandprixtracker.user.repository.ClienteRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-01T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private MetodoPagoRepository metodoPagoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    private PaymentService paymentService;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(metodoPagoRepository, clienteRepository, CLOCK);
        cliente = new Cliente(UUID.randomUUID(), "Ayrton", "Senna", "ayrton@example.com", null);
    }

    @Test
    void listarPorCliente_MarcaLasVencidas() {
        MetodoPago vigente = new MetodoPago(UUID.randomUUID(), cliente, "Credito", "4242", "tok", "12/28", null);
        MetodoPago vencida = new MetodoPago(UUID.randomUUID(), cliente, "Debito", "8812", "tok", "08/26", null);
        when(clienteRepository.existsById(cliente.getIdCliente())).thenReturn(true);
        when(metodoPagoRepository.findByClienteIdClienteOrderByCreatedAtDesc(cliente.getIdCliente()))
                .thenReturn(List.of(vigente, vencida));

        List<MetodoPagoResponseDto> metodos = paymentService.listarPorCliente(cliente.getIdCliente());

        assertFalse(metodos.get(0).vencida());
        assertTrue(metodos.get(1).vencida());
        assertEquals("4242", metodos.get(0).ultimos4Digitos());
    }

    @Test
    void listarPorCliente_ClienteInexistente_LanzaNotFound() {
        when(clienteRepository.existsById(cliente.getIdCliente())).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> paymentService.listarPorCliente(cliente.getIdCliente()));
    }

    @Test
    void cobrar_TarjetaGuardadaDelCliente_LaDevuelve() {
        MetodoPago guardado = new MetodoPago(UUID.randomUUID(), cliente, "Credito", "4242", "tok", "12/28", null);
        when(metodoPagoRepository.findByIdMetodoAndClienteIdCliente(guardado.getIdMetodo(), cliente.getIdCliente()))
                .thenReturn(Optional.of(guardado));

        MetodoPago resultado = paymentService.cobrar(cliente, new PagoRequestDto(guardado.getIdMetodo(), null, null, null, null));

        assertSame(guardado, resultado);
        verify(metodoPagoRepository, never()).save(any());
    }

    @Test
    void cobrar_TarjetaGuardadaVencida_LanzaIllegalArgument() {
        MetodoPago vencida = new MetodoPago(UUID.randomUUID(), cliente, "Debito", "8812", "tok", "08/26", null);
        when(metodoPagoRepository.findByIdMetodoAndClienteIdCliente(vencida.getIdMetodo(), cliente.getIdCliente()))
                .thenReturn(Optional.of(vencida));

        assertThrows(IllegalArgumentException.class,
                () -> paymentService.cobrar(cliente, new PagoRequestDto(vencida.getIdMetodo(), null, null, null, null)));
    }

    @Test
    void cobrar_TarjetaGuardadaDeOtroCliente_LanzaNotFound() {
        UUID idAjeno = UUID.randomUUID();
        when(metodoPagoRepository.findByIdMetodoAndClienteIdCliente(idAjeno, cliente.getIdCliente()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.cobrar(cliente, new PagoRequestDto(idAjeno, null, null, null, null)));
    }

    @Test
    void cobrar_TarjetaNueva_LaGuardaAsociadaAlCliente() {
        when(metodoPagoRepository.save(any(MetodoPago.class))).thenAnswer(inv -> inv.getArgument(0));

        MetodoPago resultado = paymentService.cobrar(cliente, new PagoRequestDto(null, "Debito", "8812", "10/26", "tok_nuevo"));

        assertSame(cliente, resultado.getCliente());
        assertEquals("8812", resultado.getUltimos4Digitos());
        assertEquals("Debito", resultado.getTipo());
    }

    @Test
    void cobrar_TarjetaVencida_LanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.cobrar(cliente, new PagoRequestDto(null, "Credito", "4242", "09/26", "tok")));
        verify(metodoPagoRepository, never()).save(any());
    }

    @Test
    void cobrar_TarjetaNuevaIncompleta_LanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.cobrar(cliente, new PagoRequestDto(null, "Credito", "4242", "12/28", null)));
    }
}
