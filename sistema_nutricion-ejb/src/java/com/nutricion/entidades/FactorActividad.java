package com.nutricion.entidades;

public enum FactorActividad {
    SEDENTARIO("Sedentario (1.2)", 1.2),
    LIGERO("Ligero (1.375)", 1.375),
    MODERADO("Moderado (1.55)", 1.55),
    INTENSO("Intenso (1.725)", 1.725),
    MUY_INTENSO("Muy intenso (1.9)", 1.9);

    private final String etiqueta;
    private final double factor;

    FactorActividad(String etiqueta, double factor) {
        this.etiqueta = etiqueta;
        this.factor = factor;
    }

    public String getEtiqueta() { return etiqueta; }
    public double getFactor() { return factor; }
}
