package com.uade.grandprixtracker.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @Column(name = "id_cliente", nullable = false, updatable = false)
    private UUID idCliente;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "apellido", nullable = false)
    private String apellido;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "telefono")
    private String telefono;

    @Column(name = "dni")
    private java.math.BigDecimal dni;

    @Column(name = "color")
    private String color;

    public Cliente() {
    }

    public Cliente(UUID idCliente, String nombre, String apellido, String email, String telefono) {
        this(idCliente, nombre, apellido, email, telefono, null, null);
    }

    public Cliente(UUID idCliente, String nombre, String apellido, String email, String telefono, java.math.BigDecimal dni) {
        this(idCliente, nombre, apellido, email, telefono, dni, null);
    }

    public Cliente(UUID idCliente, String nombre, String apellido, String email, String telefono, java.math.BigDecimal dni, String color) {
        this.idCliente = idCliente;
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.telefono = telefono;
        this.dni = dni;
        this.color = color;
    }

    public UUID getIdCliente() {
        return idCliente;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setIdCliente(UUID idCliente) {
        this.idCliente = idCliente;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public java.math.BigDecimal getDni() {
        return dni;
    }

    public void setDni(java.math.BigDecimal dni) {
        this.dni = dni;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
