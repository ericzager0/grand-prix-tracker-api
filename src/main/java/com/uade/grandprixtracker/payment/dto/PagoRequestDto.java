package com.uade.grandprixtracker.payment.dto;

import jakarta.validation.constraints.Pattern;
import java.util.UUID;

/**
 * Se envía {@code idMetodoPago} para pagar con una tarjeta guardada, o los datos de una tarjeta nueva.
 */
public record PagoRequestDto(
        UUID idMetodoPago,
        @Pattern(regexp = "Credito|Debito", message = "debe ser Credito o Debito")
        String tipo,
        @Pattern(regexp = "\\d{4}", message = "debe tener exactamente 4 dígitos")
        String ultimos4Digitos,
        @Pattern(regexp = "(0[1-9]|1[0-2])/\\d{2}", message = "debe tener formato MM/AA")
        String fechaExpiracion,
        String proveedorToken
) {}
