package com.nutricion.entidades;

public enum CategoriaArchivo {
    LABORATORIO("Laboratorio"),
    ESTUDIO("Estudio"),
    FOTO_PROGRESO("Fotografía de progreso");

    private final String etiqueta;

    CategoriaArchivo(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
