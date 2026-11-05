package com.nutricion.entidades;

public enum EstadoPago {
    PAGADO("Pagado"),
    PENDIENTE("Pendiente"),
    PARCIAL("Parcial");

    private final String etiqueta;

    EstadoPago(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Texto legible para mostrar en las vistas. */
    public String getEtiqueta() {
        return etiqueta;
    }
}
