package com.uade.grandprixtracker.booking.controller;

import com.uade.grandprixtracker.booking.dto.CheckoutRequestDto;
import com.uade.grandprixtracker.booking.dto.CheckoutResponseDto;
import com.uade.grandprixtracker.booking.service.CheckoutFacade;
import com.uade.grandprixtracker.shared.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final CheckoutFacade checkoutFacade;

    public BookingController(CheckoutFacade checkoutFacade) {
        this.checkoutFacade = checkoutFacade;
    }

    // Temporal hasta integrar Supabase Auth: el id del cliente pasa a salir del claim "sub" del JWT.
    @PostMapping
    public ResponseEntity<ApiResponse<CheckoutResponseDto>> checkout(
            @RequestHeader("X-Cliente-Id") UUID idCliente,
            @Valid @RequestBody CheckoutRequestDto request) {
        CheckoutResponseDto reserva = checkoutFacade.checkout(idCliente, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Reserva confirmada", reserva));
    }
}
