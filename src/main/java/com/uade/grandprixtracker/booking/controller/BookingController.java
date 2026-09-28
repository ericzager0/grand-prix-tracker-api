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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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

    // Temporal hasta integrar Supabase Auth: el id del cliente pasa a salir del claim "sub" del JWT.
    @PostMapping
    public ResponseEntity<ApiResponse<ReservaResponseDto>> checkout(
            @RequestHeader("X-Cliente-Id") UUID idCliente,
            @Valid @RequestBody CheckoutRequestDto request) {
        ReservaResponseDto reserva = checkoutFacade.checkout(idCliente, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Reserva confirmada", reserva));
    }

    // Temporal hasta integrar Supabase Auth: el id del cliente pasa a salir del claim "sub" del JWT.
    @GetMapping
    public ResponseEntity<ApiResponse<List<ReservaResponseDto>>> getBookings(
            @RequestHeader("X-Cliente-Id") UUID idCliente) {
        List<ReservaResponseDto> reservas = reservaService.listarPorCliente(idCliente);
        return ResponseEntity.ok(ApiResponse.success("Reservas obtenidas correctamente", reservas));
    }

    // Temporal hasta integrar Supabase Auth: el id del cliente pasa a salir del claim "sub" del JWT.
    @GetMapping("/{idReserva}")
    public ResponseEntity<ApiResponse<ReservaResponseDto>> getBooking(
            @RequestHeader("X-Cliente-Id") UUID idCliente,
            @PathVariable UUID idReserva) {
        ReservaResponseDto reserva = reservaService.obtener(idCliente, idReserva);
        return ResponseEntity.ok(ApiResponse.success("Reserva obtenida correctamente", reserva));
    }
}
