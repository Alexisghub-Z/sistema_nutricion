package com.nutricion.entidades;

public enum OrigenCita {
    PANEL("Panel"),
    PORTAL("Portal web"),
    WHATSAPP("WhatsApp");

    private final String etiqueta;

    OrigenCita(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
