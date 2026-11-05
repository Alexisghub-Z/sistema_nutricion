package com.nutricion.entidades;

public enum Sexo {
    FEMENINO("Femenino"),
    MASCULINO("Masculino");

    private final String etiqueta;

    Sexo(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Texto legible para mostrar en las vistas. */
    public String getEtiqueta() {
        return etiqueta;
    }
}
