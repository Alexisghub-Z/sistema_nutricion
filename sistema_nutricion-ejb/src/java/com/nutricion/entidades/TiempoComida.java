package com.nutricion.entidades;

public enum TiempoComida {
    DESAYUNO("Desayuno"),
    COLACION_MATUTINA("Colación matutina"),
    COMIDA("Comida"),
    COLACION_VESPERTINA("Colación vespertina"),
    CENA("Cena");

    private final String etiqueta;

    TiempoComida(String etiqueta) { this.etiqueta = etiqueta; }

    public String getEtiqueta() { return etiqueta; }
}
