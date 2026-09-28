package com.uade.grandprixtracker.booking.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.uade.grandprixtracker.booking.dto.CheckoutRequestDto;
import com.uade.grandprixtracker.booking.dto.ReservaResponseDto;
import com.uade.grandprixtracker.booking.service.CheckoutFacade;
import com.uade.grandprixtracker.booking.service.ReservaService;
import com.uade.grandprixtracker.shared.exception.GlobalExceptionHandler;
import com.uade.grandprixtracker.shared.exception.ResourceNotFoundException;
import com.uade.grandprixtracker.shared.exception.StockInsuficienteException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CheckoutFacade checkoutFacade;

    @Mock
    private ReservaService reservaService;

    @InjectMocks
    private BookingController bookingController;

    private UUID idCliente;
    private UUID idEvento;
    private UUID idEntrada;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(bookingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        idCliente = UUID.randomUUID();
        idEvento = UUID.randomUUID();
        idEntrada = UUID.randomUUID();
    }

    private String body(int cantidad) {
        return """
                {
                  "idEvento": "%s",
                  "entradas": [{ "idEntrada": "%s", "cantidad": %d }],
                  "pago": { "tipo": "Credito", "ultimos4Digitos": "4242", "fechaExpiracion": "12/28", "proveedorToken": "tok_test" }
                }
                """.formatted(idEvento, idEntrada, cantidad);
    }

    private ReservaResponseDto reserva(UUID idReserva, String codigo, String fechaCompra) {
        UUID idMetodoPago = UUID.randomUUID();
        return new ReservaResponseDto(
                idReserva, codigo, "Pagada", OffsetDateTime.parse(fechaCompra),
                new BigDecimal("1500.00"), true, true, false, idMetodoPago,
                new ReservaResponseDto.MetodoPagoResumen(idMetodoPago, "Credito", "4242"),
                new ReservaResponseDto.EventoResumen(idEvento, 2026, LocalDate.of(2026, 11, 6), LocalDate.of(2026, 11, 8),
                        new ReservaResponseDto.CircuitoResumen("Interlagos",
                                new ReservaResponseDto.CiudadResumen("Sao Paulo",
                                        new ReservaResponseDto.PaisResumen("Brasil", "BR")))),
                List.of(new ReservaResponseDto.EntradaLinea(idEntrada, "Tribuna A", "General", 2,
                        new BigDecimal("350.00"), new BigDecimal("700.00"))),
                List.of(new ReservaResponseDto.HabitacionLinea(UUID.randomUUID(), UUID.randomUUID(), "Hotel Paulista", "Doble",
                        LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 9), 4,
                        new BigDecimal("200.00"), new BigDecimal("800.00"))),
                List.of());
    }

    @Test
    void checkout_RequestValida_Devuelve201ConLaReservaYLosCamposNuevos() throws Exception {
        ReservaResponseDto respuesta = reserva(UUID.randomUUID(), "GP-ABCD2345", "2026-10-01T12:00:00Z");
        when(checkoutFacade.checkout(eq(idCliente), any(CheckoutRequestDto.class))).thenReturn(respuesta);

        mockMvc.perform(post("/bookings")
                        .header("X-Cliente-Id", idCliente.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Reserva confirmada"))
                // Contrato previo del POST
                .andExpect(jsonPath("$.data.idReserva").value(respuesta.idReserva().toString()))
                .andExpect(jsonPath("$.data.codigoConfirmacion").value("GP-ABCD2345"))
                .andExpect(jsonPath("$.data.estado").value("Pagada"))
                .andExpect(jsonPath("$.data.fechaCompra").exists())
                .andExpect(jsonPath("$.data.totalUsd").value(1500.00))
                .andExpect(jsonPath("$.data.incluyeEntrada").value(true))
                .andExpect(jsonPath("$.data.incluyeHotel").value(true))
                .andExpect(jsonPath("$.data.incluyeVuelo").value(false))
                .andExpect(jsonPath("$.data.idMetodoPago").value(respuesta.idMetodoPago().toString()))
                .andExpect(jsonPath("$.data.entradas[0].cantidad").value(2))
                .andExpect(jsonPath("$.data.entradas[0].precioUnitarioUsd").value(350.00))
                .andExpect(jsonPath("$.data.habitaciones[0].hotel").value("Hotel Paulista"))
                .andExpect(jsonPath("$.data.habitaciones[0].cantidadNoches").value(4))
                .andExpect(jsonPath("$.data.vuelos").isArray())
                // Campos nuevos
                .andExpect(jsonPath("$.data.habitaciones[0].idHotel").exists())
                .andExpect(jsonPath("$.data.metodoPago.tipo").value("Credito"))
                .andExpect(jsonPath("$.data.metodoPago.ultimos4Digitos").value("4242"))
                .andExpect(jsonPath("$.data.metodoPago.proveedorToken").doesNotExist())
                .andExpect(jsonPath("$.data.evento.idEvento").value(idEvento.toString()))
                .andExpect(jsonPath("$.data.evento.fechaInicio").value("2026-11-06"))
                .andExpect(jsonPath("$.data.evento.circuito.nombre").value("Interlagos"))
                .andExpect(jsonPath("$.data.evento.circuito.ciudad.nombre").value("Sao Paulo"))
                .andExpect(jsonPath("$.data.evento.circuito.ciudad.pais.codigoIso").value("BR"));
    }

    @Test
    void checkout_SinHeaderCliente_Devuelve400() throws Exception {
        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(checkoutFacade);
    }

    @Test
    void checkout_CantidadInvalida_Devuelve400() throws Exception {
        mockMvc.perform(post("/bookings")
                        .header("X-Cliente-Id", idCliente.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(checkoutFacade);
    }

    @Test
    void checkout_SinStock_Devuelve409() throws Exception {
        when(checkoutFacade.checkout(eq(idCliente), any(CheckoutRequestDto.class)))
                .thenThrow(new StockInsuficienteException("No hay stock suficiente para la tribuna Tribuna A"));

        mockMvc.perform(post("/bookings")
                        .header("X-Cliente-Id", idCliente.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("No hay stock suficiente para la tribuna Tribuna A"));
    }

    @Test
    void getBookings_SinReservas_Devuelve200ConListaVacia() throws Exception {
        when(reservaService.listarPorCliente(idCliente)).thenReturn(List.of());

        mockMvc.perform(get("/bookings").header("X-Cliente-Id", idCliente.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void getBookings_ConReservas_DevuelveLasReservasEnElOrdenDelService() throws Exception {
        when(reservaService.listarPorCliente(idCliente)).thenReturn(List.of(
                reserva(UUID.randomUUID(), "GP-NUEVA234", "2026-10-02T12:00:00Z"),
                reserva(UUID.randomUUID(), "GP-VIEJA234", "2026-09-01T12:00:00Z")));

        mockMvc.perform(get("/bookings").header("X-Cliente-Id", idCliente.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].codigoConfirmacion").value("GP-NUEVA234"))
                .andExpect(jsonPath("$.data[1].codigoConfirmacion").value("GP-VIEJA234"));
    }

    @Test
    void getBookings_SinHeaderCliente_Devuelve400() throws Exception {
        mockMvc.perform(get("/bookings"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Falta el header requerido: X-Cliente-Id"));

        verifyNoInteractions(reservaService);
    }

    @Test
    void getBookings_HeaderClienteNoUuid_Devuelve400() throws Exception {
        mockMvc.perform(get("/bookings").header("X-Cliente-Id", "no-es-un-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(reservaService);
    }

    @Test
    void getBookings_ClienteInexistente_Devuelve404() throws Exception {
        when(reservaService.listarPorCliente(idCliente)).thenThrow(new ResourceNotFoundException("Cliente no encontrado"));

        mockMvc.perform(get("/bookings").header("X-Cliente-Id", idCliente.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cliente no encontrado"));
    }

    @Test
    void getBooking_ReservaDelCliente_Devuelve200ConElDetalle() throws Exception {
        UUID idReserva = UUID.randomUUID();
        when(reservaService.obtener(idCliente, idReserva)).thenReturn(reserva(idReserva, "GP-ABCD2345", "2026-10-01T12:00:00Z"));

        mockMvc.perform(get("/bookings/{idReserva}", idReserva).header("X-Cliente-Id", idCliente.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.idReserva").value(idReserva.toString()))
                .andExpect(jsonPath("$.data.habitaciones[0].fechaCheckIn").value("2026-11-05"))
                .andExpect(jsonPath("$.data.metodoPago.ultimos4Digitos").value("4242"));
    }

    @Test
    void getBooking_ReservaDeOtroClienteOInexistente_Devuelve404() throws Exception {
        UUID idReserva = UUID.randomUUID();
        when(reservaService.obtener(idCliente, idReserva)).thenThrow(new ResourceNotFoundException("Reserva no encontrada"));

        mockMvc.perform(get("/bookings/{idReserva}", idReserva).header("X-Cliente-Id", idCliente.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Reserva no encontrada"));
    }

    @Test
    void getBooking_IdReservaNoUuid_Devuelve400() throws Exception {
        mockMvc.perform(get("/bookings/{idReserva}", "123").header("X-Cliente-Id", idCliente.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Valor inválido para idReserva"));

        verifyNoInteractions(reservaService);
    }

    @Test
    void getBooking_SinHeaderCliente_Devuelve400() throws Exception {
        mockMvc.perform(get("/bookings/{idReserva}", UUID.randomUUID()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reservaService);
    }
}
