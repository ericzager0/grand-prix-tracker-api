package com.uade.grandprixtracker.notification.email;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uade.grandprixtracker.booking.dto.ReservaResponseDto;
import com.uade.grandprixtracker.booking.event.CompraConfirmadaEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompraConfirmadaEmailListenerTest {

    @Mock
    private BrevoEmailSender emailSender;

    private EmailTemplateRenderer templateRenderer;
    private CompraConfirmadaEmailListener listener;

    @BeforeEach
    void setUp() {
        templateRenderer = new EmailTemplateRenderer();
        listener = new CompraConfirmadaEmailListener(emailSender, templateRenderer);
        when(emailSender.isConfigured()).thenReturn(true);
    }

    private ReservaResponseDto crearReserva(boolean incluyeTransporte) {
        UUID idMetodo = UUID.randomUUID();
        return new ReservaResponseDto(
                UUID.randomUUID(),
                "GP-TEST1234",
                "Pagada",
                OffsetDateTime.now(),
                incluyeTransporte ? new BigDecimal("2530.00") : new BigDecimal("2500.00"),
                true,
                false,
                false,
                incluyeTransporte,
                idMetodo,
                new ReservaResponseDto.MetodoPagoResumen(idMetodo, "Credito", "4242"),
                new ReservaResponseDto.EventoResumen(
                        UUID.randomUUID(),
                        2026,
                        LocalDate.of(2026, 11, 6),
                        LocalDate.of(2026, 11, 8),
                        new ReservaResponseDto.CircuitoResumen(
                                "Interlagos",
                                new ReservaResponseDto.CiudadResumen("Sao Paulo",
                                        new ReservaResponseDto.PaisResumen("Brasil", "BR")))),
                List.of(new ReservaResponseDto.EntradaLinea(
                        UUID.randomUUID(), "Tribuna A", "General", 2, new BigDecimal("1250.00"),
                        new BigDecimal("2500.00"))),
                List.of(),
                List.of());
    }

    @Test
    void onCompraConfirmada_ConTransporte_IncluyeTransladoYEsteticaCorrecta() {
        ReservaResponseDto reserva = crearReserva(true);
        CompraConfirmadaEvent event = new CompraConfirmadaEvent("test@example.com", "Ayrton Senna", reserva);

        listener.onCompraConfirmada(event);

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq("test@example.com"), eq("Ayrton Senna"), anyString(), htmlCaptor.capture());

        String html = htmlCaptor.getValue();

        // 1) Rojo #E10600 y barra inferior #0B0B10
        assertTrue(html.contains("#E10600"));
        assertTrue(html.contains("#0B0B10"));

        // 2) Translado con 30 USD
        assertTrue(html.contains("Translado"));
        assertTrue(html.contains("US$ 30.00"));

        // 3) Recuadro más ancho (700px)
        assertTrue(html.contains("width=\"700\""));
        assertTrue(html.contains("max-width:700px"));

        // 4) Logo de la app presente en el template vía HTTPS
        assertTrue(html.contains(
                "src=\"https://raw.githubusercontent.com/ericzager0/grand-prix-tracker-api/main/src/assets/logo-nobg.png\""));
        assertTrue(html.contains("alt=\"Grand Prix Tracker\""));

        // 5) Tamaño del HTML < 10KB (lejos del límite de 102KB de Gmail para evitar
        // recorte)
        assertTrue(html.length() < 10000);
    }

    @Test
    void onCompraConfirmada_SinTransporte_NoIncluyeFilaTranslado() {
        ReservaResponseDto reserva = crearReserva(false);
        CompraConfirmadaEvent event = new CompraConfirmadaEvent("test@example.com", "Ayrton Senna", reserva);

        listener.onCompraConfirmada(event);

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq("test@example.com"), eq("Ayrton Senna"), anyString(), htmlCaptor.capture());

        String html = htmlCaptor.getValue();

        assertFalse(html.contains("Translado"));
        assertFalse(html.contains("Transporte al circuito"));
        assertTrue(html.contains("#E10600"));
        assertTrue(html.contains("#0B0B10"));
        assertTrue(html.contains("width=\"700\""));
        assertTrue(html.contains(
                "src=\"https://raw.githubusercontent.com/ericzager0/grand-prix-tracker-api/main/src/assets/logo-nobg.png\""));
        assertTrue(html.length() < 10000);
    }
}
