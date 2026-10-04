package com.uade.grandprixtracker.notification.email;

import com.uade.grandprixtracker.booking.dto.ReservaResponseDto;
import com.uade.grandprixtracker.booking.dto.ReservaResponseDto.EntradaLinea;
import com.uade.grandprixtracker.booking.dto.ReservaResponseDto.HabitacionLinea;
import com.uade.grandprixtracker.booking.dto.ReservaResponseDto.VueloLinea;
import com.uade.grandprixtracker.booking.event.CompraConfirmadaEvent;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.HtmlUtils;

/**
 * Envía el email con el detalle de la compra una vez confirmada.
 *
 * Garantías para que la compra NUNCA falle por el email:
 *  - AFTER_COMMIT: solo se ejecuta si la transacción del checkout ya se guardó en la BD.
 *  - @Async: corre en otro hilo; el usuario recibe la respuesta sin esperar a Brevo.
 *  - try/catch: cualquier error (Brevo caído, API key inválida, etc.) solo se loguea.
 */
@Component
public class CompraConfirmadaEmailListener {

    private static final Logger log = LoggerFactory.getLogger(CompraConfirmadaEmailListener.class);
    private static final String TEMPLATE = "compra-confirmada.html";
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String DEFAULT_LOGO_URL = "https://raw.githubusercontent.com/ericzager0/grand-prix-tracker-api/main/src/assets/logo-nobg.png";

    private final BrevoEmailSender emailSender;
    private final EmailTemplateRenderer templateRenderer;
    private final String logoUrl;

    @Autowired
    public CompraConfirmadaEmailListener(
            BrevoEmailSender emailSender,
            EmailTemplateRenderer templateRenderer,
            @Value("${app.email.logo-url:" + DEFAULT_LOGO_URL + "}") String logoUrl) {
        this.emailSender = emailSender;
        this.templateRenderer = templateRenderer;
        this.logoUrl = (logoUrl != null && !logoUrl.isBlank()) ? logoUrl : DEFAULT_LOGO_URL;
    }

    public CompraConfirmadaEmailListener(BrevoEmailSender emailSender, EmailTemplateRenderer templateRenderer) {
        this(emailSender, templateRenderer, DEFAULT_LOGO_URL);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCompraConfirmada(CompraConfirmadaEvent event) {
        ReservaResponseDto reserva = event.reserva();

        if (!emailSender.isConfigured()) {
            log.warn("Brevo no está configurado (BREVO_API_KEY / BREVO_SENDER_EMAIL). No se envía email de la reserva {}",
                    reserva.codigoConfirmacion());
            return;
        }

        try {
            String html = templateRenderer.render(TEMPLATE, variables(event));
            String asunto = "Confirmación de compra " + reserva.codigoConfirmacion() + " - Grand Prix Tracker";
            emailSender.send(event.emailCliente(), event.nombreCliente(), asunto, html);
            log.info("Email de confirmación enviado a {} para la reserva {}", event.emailCliente(), reserva.codigoConfirmacion());
        } catch (Exception e) {
            log.error("No se pudo enviar el email de la reserva {} a {}: {}",
                    reserva.codigoConfirmacion(), event.emailCliente(), e.getMessage(), e);
        }
    }

    private Map<String, String> variables(CompraConfirmadaEvent event) {
        ReservaResponseDto r = event.reserva();
        String logoHtml = (logoUrl != null && !logoUrl.isBlank())
                ? "<img src=\"" + esc(logoUrl) + "\" alt=\"Grand Prix Tracker\" width=\"60\" height=\"65\" style=\"display:block;margin:0 auto;max-width:60px;height:auto;border:0;\" />"
                : "";

        return Map.of(
                "nombre", esc(event.nombreCliente()),
                "codigo", esc(r.codigoConfirmacion()),
                "fechaCompra", r.fechaCompra() != null ? r.fechaCompra().format(FECHA_HORA) : "",
                "evento", esc(nombreEvento(r)),
                "fechasEvento", fechasEvento(r),
                "detalle", filasDetalle(r),
                "total", usd(r.totalUsd()),
                "metodoPago", metodoPago(r),
                "logo", logoHtml
        );
    }

    private String filasDetalle(ReservaResponseDto r) {
        StringBuilder filas = new StringBuilder();
        if (r.entradas() != null) {
            for (EntradaLinea e : r.entradas()) {
                filas.append(fila("Entrada " + e.nombreTribuna() + " (" + e.tipo() + ") x" + e.cantidad(), e.subtotalUsd()));
            }
        }
        if (r.habitaciones() != null) {
            for (HabitacionLinea h : r.habitaciones()) {
                filas.append(fila("Hotel " + h.hotel() + " - " + h.tipo() + " (" + h.cantidadNoches() + " noches, "
                        + h.fechaCheckIn().format(FECHA) + " al " + h.fechaCheckOut().format(FECHA) + ")", h.subtotalUsd()));
            }
        }
        if (r.vuelos() != null) {
            for (VueloLinea v : r.vuelos()) {
                filas.append(fila("Vuelo " + v.aerolinea() + " " + v.origen() + " → " + v.destino()
                        + " (" + v.fechaSalida().format(FECHA_HORA) + ") x" + v.cantidadPasajeros(), v.subtotalUsd()));
            }
        }
        if (r.incluyeTransporte()) {
            filas.append(fila("Translado", BigDecimal.valueOf(30)));
        }
        return filas.toString();
    }

    private String fila(String descripcion, BigDecimal subtotal) {
        return "<tr><td style=\"padding:8px 0;border-bottom:1px solid #eee;\">" + esc(descripcion) + "</td>"
                + "<td style=\"padding:8px 0;border-bottom:1px solid #eee;text-align:right;white-space:nowrap;\">"
                + (subtotal != null ? usd(subtotal) : "Incluido") + "</td></tr>";
    }

    private String nombreEvento(ReservaResponseDto r) {
        if (r.evento() == null || r.evento().circuito() == null) {
            return "Gran Premio";
        }
        var circuito = r.evento().circuito();
        String ciudad = circuito.ciudad() != null ? " - " + circuito.ciudad().nombre() : "";
        return circuito.nombre() + ciudad + " " + r.evento().temporada();
    }

    private String fechasEvento(ReservaResponseDto r) {
        if (r.evento() == null || r.evento().fechaInicio() == null) {
            return "";
        }
        return r.evento().fechaInicio().format(FECHA) + " al " + r.evento().fechaFin().format(FECHA);
    }

    private String metodoPago(ReservaResponseDto r) {
        if (r.metodoPago() == null) {
            return "";
        }
        return esc(r.metodoPago().tipo()) + " terminada en " + esc(r.metodoPago().ultimos4Digitos());
    }

    private static String usd(BigDecimal monto) {
        return monto != null ? "US$ " + monto.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() : "";
    }

    private static String esc(String value) {
        return value != null ? HtmlUtils.htmlEscape(value) : "";
    }
}
