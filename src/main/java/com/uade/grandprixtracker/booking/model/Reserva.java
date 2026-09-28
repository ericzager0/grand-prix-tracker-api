package com.uade.grandprixtracker.booking.model;

import com.uade.grandprixtracker.event.model.EventoF1;
import com.uade.grandprixtracker.payment.model.MetodoPago;
import com.uade.grandprixtracker.user.model.Cliente;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reservas")
public class Reserva {

    public static final String ESTADO_PAGADA = "Pagada";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_reserva", nullable = false, updatable = false)
    private UUID idReserva;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_metodo_pago")
    private MetodoPago metodoPago;

    // Nullable: las reservas anteriores a la columna cuyo evento no se pudo deducir quedan sin evento.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento")
    private EventoF1 evento;

    @Column(name = "codigo_confirmacion", nullable = false, unique = true)
    private String codigoConfirmacion;

    @Column(name = "total_usd", nullable = false)
    private BigDecimal totalUsd = BigDecimal.ZERO;

    @Column(name = "estado")
    private String estado;

    @Column(name = "fecha_compra")
    private OffsetDateTime fechaCompra;

    @Column(name = "incluye_hotel")
    private boolean incluyeHotel;

    @Column(name = "incluye_entrada")
    private boolean incluyeEntrada;

    @Column(name = "incluye_vuelo")
    private boolean incluyeVuelo;

    @Column(name = "incluye_transporte")
    private boolean incluyeTransporte;

    @OneToMany(mappedBy = "reserva", cascade = CascadeType.PERSIST)
    private List<ReservaDetalleEntrada> entradas = new ArrayList<>();

    @OneToMany(mappedBy = "reserva", cascade = CascadeType.PERSIST)
    private List<ReservaDetalleHotel> habitaciones = new ArrayList<>();

    @OneToMany(mappedBy = "reserva", cascade = CascadeType.PERSIST)
    private List<ReservaDetalleVuelo> vuelos = new ArrayList<>();

    public Reserva() {
    }

    public Reserva(Cliente cliente, EventoF1 evento, MetodoPago metodoPago, String codigoConfirmacion, String estado, OffsetDateTime fechaCompra) {
        this.cliente = cliente;
        this.evento = evento;
        this.metodoPago = metodoPago;
        this.codigoConfirmacion = codigoConfirmacion;
        this.estado = estado;
        this.fechaCompra = fechaCompra;
    }

    public void agregarEntrada(ReservaDetalleEntrada detalle) {
        detalle.setReserva(this);
        entradas.add(detalle);
        incluyeEntrada = true;
        totalUsd = totalUsd.add(detalle.getSubtotalUsd());
    }

    public void agregarHabitacion(ReservaDetalleHotel detalle) {
        detalle.setReserva(this);
        habitaciones.add(detalle);
        incluyeHotel = true;
        totalUsd = totalUsd.add(detalle.getSubtotalUsd());
    }

    public void agregarVuelo(ReservaDetalleVuelo detalle) {
        detalle.setReserva(this);
        vuelos.add(detalle);
        incluyeVuelo = true;
        totalUsd = totalUsd.add(detalle.getSubtotalUsd());
    }

    public UUID getIdReserva() {
        return idReserva;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public EventoF1 getEvento() {
        return evento;
    }

    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    public String getCodigoConfirmacion() {
        return codigoConfirmacion;
    }

    public BigDecimal getTotalUsd() {
        return totalUsd;
    }

    public String getEstado() {
        return estado;
    }

    public OffsetDateTime getFechaCompra() {
        return fechaCompra;
    }

    public boolean isIncluyeHotel() {
        return incluyeHotel;
    }

    public boolean isIncluyeEntrada() {
        return incluyeEntrada;
    }

    public boolean isIncluyeVuelo() {
        return incluyeVuelo;
    }

    public boolean isIncluyeTransporte() {
        return incluyeTransporte;
    }

    public List<ReservaDetalleEntrada> getEntradas() {
        return entradas;
    }

    public List<ReservaDetalleHotel> getHabitaciones() {
        return habitaciones;
    }

    public List<ReservaDetalleVuelo> getVuelos() {
        return vuelos;
    }
}
