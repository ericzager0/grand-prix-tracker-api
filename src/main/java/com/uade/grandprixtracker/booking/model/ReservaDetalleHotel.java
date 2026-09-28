package com.uade.grandprixtracker.booking.model;

import com.uade.grandprixtracker.hotel.model.HabitacionHotel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "reserva_detalle_hoteles")
public class ReservaDetalleHotel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_detalle_hotel", nullable = false, updatable = false)
    private UUID idDetalleHotel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_reserva", nullable = false)
    private Reserva reserva;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_habitacion", nullable = false)
    private HabitacionHotel habitacion;

    @Column(name = "fecha_check_in", nullable = false)
    private LocalDate fechaCheckIn;

    @Column(name = "fecha_check_out", nullable = false)
    private LocalDate fechaCheckOut;

    @Column(name = "cantidad_noches", nullable = false)
    private Integer cantidadNoches;

    @Column(name = "subtotal_usd", nullable = false)
    private BigDecimal subtotalUsd;

    public ReservaDetalleHotel() {
    }

    public ReservaDetalleHotel(HabitacionHotel habitacion, LocalDate fechaCheckIn, LocalDate fechaCheckOut, Integer cantidadNoches, BigDecimal subtotalUsd) {
        this.habitacion = habitacion;
        this.fechaCheckIn = fechaCheckIn;
        this.fechaCheckOut = fechaCheckOut;
        this.cantidadNoches = cantidadNoches;
        this.subtotalUsd = subtotalUsd;
    }

    void setReserva(Reserva reserva) {
        this.reserva = reserva;
    }

    public HabitacionHotel getHabitacion() {
        return habitacion;
    }

    public LocalDate getFechaCheckIn() {
        return fechaCheckIn;
    }

    public LocalDate getFechaCheckOut() {
        return fechaCheckOut;
    }

    public Integer getCantidadNoches() {
        return cantidadNoches;
    }

    public BigDecimal getSubtotalUsd() {
        return subtotalUsd;
    }
}
