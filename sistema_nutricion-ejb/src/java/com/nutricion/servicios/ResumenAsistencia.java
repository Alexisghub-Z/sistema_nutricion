package com.nutricion.servicios;

/** Asistencia registrada para un paciente en el periodo del panel. */
public final class ResumenAsistencia {
    public static final int UMBRAL_ASISTENCIA_BAJA = 80;

    private final String paciente;
    private final long citas;
    private final long asistidas;

    public ResumenAsistencia(String paciente, Long citas, Long asistidas) {
        this.paciente = paciente;
        this.citas = citas;
        this.asistidas = asistidas;
    }

    public String getPaciente() { return paciente; }
    public long getAsistidas() { return asistidas; }
    public long getAusencias() { return citas - asistidas; }
    public long getPorcentaje() { return Math.round(100.0 * asistidas / citas); }
    public boolean isAsistenciaBaja() { return getPorcentaje() < UMBRAL_ASISTENCIA_BAJA; }
}
