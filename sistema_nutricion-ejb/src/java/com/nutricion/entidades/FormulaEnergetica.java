package com.nutricion.entidades;

public enum FormulaEnergetica {
    MIFFLIN_ST_JEOR("Mifflin-St Jeor", false),
    HARRIS_BENEDICT("Harris-Benedict", false),
    KATCH_MCARDLE("Katch-McArdle", true),
    CUNNINGHAM("Cunningham", true),
    FAO_OMS("FAO/OMS", false);

    private final String etiqueta;
    private final boolean requiereGrasa;

    FormulaEnergetica(String etiqueta, boolean requiereGrasa) {
        this.etiqueta = etiqueta;
        this.requiereGrasa = requiereGrasa;
    }

    public String getEtiqueta() { return etiqueta; }
    public boolean isRequiereGrasa() { return requiereGrasa; }
}
