package com.nutricion.entidades;

public enum TipoCita {
    PRESENCIAL("Presencial"),
    EN_LINEA("En línea");

    private final String etiqueta;

    TipoCita(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Texto legible para mostrar en las vistas. */
    public String getEtiqueta() {
        return etiqueta;
    }
}
