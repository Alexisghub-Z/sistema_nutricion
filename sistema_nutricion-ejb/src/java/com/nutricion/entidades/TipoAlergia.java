package com.nutricion.entidades;

public enum TipoAlergia {
    ALERGIA("Alergia"),
    INTOLERANCIA("Intolerancia");

    private final String etiqueta;

    TipoAlergia(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
