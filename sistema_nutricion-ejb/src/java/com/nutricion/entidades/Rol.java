package com.nutricion.entidades;

public enum Rol {
    ADMIN("Administrador"),
    NUTRIOLOGO("Nutriólogo"),
    RECEPCION("Recepción");

    private final String etiqueta;

    Rol(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Texto legible para mostrar en las vistas. */
    public String getEtiqueta() {
        return etiqueta;
    }
}
