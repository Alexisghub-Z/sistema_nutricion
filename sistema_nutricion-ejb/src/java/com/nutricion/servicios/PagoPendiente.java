package com.nutricion.servicios;

import com.nutricion.entidades.EstadoPago;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/** Datos de cobro que puede consultar recepción. */
public final class PagoPendiente implements Serializable {

    private static final long serialVersionUID = 1L;
    private final Long id;
    private final String nombrePaciente;
    private final Date fecha;
    private final BigDecimal monto;
    private final EstadoPago estado;

    public PagoPendiente(Long id, String nombrePaciente, Date fecha, BigDecimal monto, EstadoPago estado) {
        this.id = id;
        this.nombrePaciente = nombrePaciente;
        this.fecha = fecha;
        this.monto = monto;
        this.estado = estado;
    }

    public Long getId() { return id; }
    public String getNombrePaciente() { return nombrePaciente; }
    public Date getFecha() { return fecha; }
    public BigDecimal getMonto() { return monto; }
    public EstadoPago getEstado() { return estado; }
}
