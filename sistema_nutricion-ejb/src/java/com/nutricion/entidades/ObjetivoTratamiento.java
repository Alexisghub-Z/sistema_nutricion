package com.nutricion.entidades;

public enum ObjetivoTratamiento {
    PERDIDA("Pérdida de peso", -500, 25, 25, 50),
    MANTENIMIENTO("Mantenimiento", 0, 20, 30, 50),
    GANANCIA("Ganancia muscular", 300, 25, 25, 50);

    private final String etiqueta;
    private final int ajusteSugerido;
    private final int pctProteinasSugerido;
    private final int pctLipidosSugerido;
    private final int pctCarbohidratosSugerido;

    ObjetivoTratamiento(String etiqueta, int ajusteSugerido, int pctProt, int pctLip, int pctHc) {
        this.etiqueta = etiqueta;
        this.ajusteSugerido = ajusteSugerido;
        this.pctProteinasSugerido = pctProt;
        this.pctLipidosSugerido = pctLip;
        this.pctCarbohidratosSugerido = pctHc;
    }

    public String getEtiqueta() { return etiqueta; }
    public int getAjusteSugerido() { return ajusteSugerido; }
    public int getPctProteinasSugerido() { return pctProteinasSugerido; }
    public int getPctLipidosSugerido() { return pctLipidosSugerido; }
    public int getPctCarbohidratosSugerido() { return pctCarbohidratosSugerido; }
}
