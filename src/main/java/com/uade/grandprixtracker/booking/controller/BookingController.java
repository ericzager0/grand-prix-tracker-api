package com.uade.grandprixtracker.booking.controller;

import com.uade.grandprixtracker.booking.dto.CheckoutRequestDto;
import com.uade.grandprixtracker.booking.dto.ReservaResponseDto;
import com.uade.grandprixtracker.booking.service.CheckoutFacade;
import com.uade.grandprixtracker.booking.service.ReservaService;
import com.uade.grandprixtracker.shared.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final CheckoutFacade checkoutFacade;
    private final ReservaService reservaService;

    public BookingController(CheckoutFacade checkoutFacade, ReservaService reservaService) {
        this.checkoutFacade = checkoutFacade;
        this.reservaService = reservaService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReservaResponseDto>> checkout(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CheckoutRequestDto request) {
        ReservaResponseDto reserva = checkoutFacade.checkout(idCliente(jwt), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Reserva confirmada", reserva));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReservaResponseDto>>> getBookings(
            @AuthenticationPrincipal Jwt jwt) {
        List<ReservaResponseDto> reservas = reservaService.listarPorCliente(idCliente(jwt));
        return ResponseEntity.ok(ApiResponse.success("Reservas obtenidas correctamente", reservas));
    }

    @GetMapping("/{idReserva}")
    public ResponseEntity<ApiResponse<ReservaResponseDto>> getBooking(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID idReserva) {
        ReservaResponseDto reserva = reservaService.obtener(idCliente(jwt), idReserva);
        return ResponseEntity.ok(ApiResponse.success("Reserva obtenida correctamente", reserva));
    }

    // El claim "sub" del JWT de Supabase es el id del usuario, que coincide con clientes.id_cliente.
    private static UUID idCliente(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
