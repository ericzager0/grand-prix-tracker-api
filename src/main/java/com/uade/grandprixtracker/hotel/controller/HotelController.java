package com.uade.grandprixtracker.hotel.controller;

import com.uade.grandprixtracker.hotel.dto.HotelResponseDto;
import com.uade.grandprixtracker.hotel.service.HotelService;
import com.uade.grandprixtracker.shared.response.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HotelController {

    private final HotelService hotelService;

    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @GetMapping("/events/{idEvento}/hotels")
    public ResponseEntity<ApiResponse<List<HotelResponseDto>>> getHotelsByEvent(@PathVariable UUID idEvento) {
        List<HotelResponseDto> hoteles = hotelService.listarPorEvento(idEvento);
        return ResponseEntity.ok(ApiResponse.success("Hoteles obtenidos correctamente", hoteles));
    }
}
