package com.uade.grandprixtracker.payment.model;

import com.uade.grandprixtracker.user.model.Cliente;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "metodos_pago")
public class MetodoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_metodo", nullable = false, updatable = false)
    private UUID idMetodo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @Column(name = "tipo", nullable = false)
    private String tipo;

    @Column(name = "ultimos_4_digitos", nullable = false)
    private String ultimos4Digitos;

    @Column(name = "proveedor_token", nullable = false, columnDefinition = "text")
    private String proveedorToken;

    @Column(name = "fecha_expiracion", nullable = false)
    private String fechaExpiracion;

    @Column(name = "nombre_titular")
    private String nombreTitular;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public MetodoPago() {
    }

    public MetodoPago(UUID idMetodo, Cliente cliente, String tipo, String ultimos4Digitos, String proveedorToken, String fechaExpiracion, OffsetDateTime createdAt) {
        this(idMetodo, cliente, tipo, ultimos4Digitos, proveedorToken, fechaExpiracion, createdAt, null);
    }

    public MetodoPago(UUID idMetodo, Cliente cliente, String tipo, String ultimos4Digitos, String proveedorToken, String fechaExpiracion, OffsetDateTime createdAt, String nombreTitular) {
        this.idMetodo = idMetodo;
        this.cliente = cliente;
        this.tipo = tipo;
        this.ultimos4Digitos = ultimos4Digitos;
        this.proveedorToken = proveedorToken;
        this.fechaExpiracion = fechaExpiracion;
        this.createdAt = createdAt;
        this.nombreTitular = nombreTitular;
    }

    public UUID getIdMetodo() {
        return idMetodo;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public String getTipo() {
        return tipo;
    }

    public String getUltimos4Digitos() {
        return ultimos4Digitos;
    }

    public String getProveedorToken() {
        return proveedorToken;
    }

    public String getFechaExpiracion() {
        return fechaExpiracion;
    }

    public String getNombreTitular() {
        return nombreTitular;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
