package com.nutricion.servicios;

import java.util.Calendar;
import java.util.Date;

/** Utilidades de rangos de fecha para las consultas JPQL. */
public final class Fechas {

    private Fechas() {
    }

    public static Date inicioDelDia(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    public static Date sumarDias(Date d, int dias) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.add(Calendar.DAY_OF_MONTH, dias);
        return c.getTime();
    }

    public static Date sumarMinutos(Date d, int minutos) {
        return new Date(d.getTime() + minutos * 60_000L);
    }

    public static Date inicioDelMes(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(inicioDelDia(d));
        c.set(Calendar.DAY_OF_MONTH, 1);
        return c.getTime();
    }

    public static Date inicioMesSiguiente(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(inicioDelMes(d));
        c.add(Calendar.MONTH, 1);
        return c.getTime();
    }

    /** Fecha con hora concreta, útil para datos de ejemplo. */
    public static Date en(int diasDesdeHoy, int hora, int minuto) {
        Calendar c = Calendar.getInstance();
        c.setTime(sumarDias(inicioDelDia(new Date()), diasDesdeHoy));
        c.set(Calendar.HOUR_OF_DAY, hora);
        c.set(Calendar.MINUTE, minuto);
        return c.getTime();
    }
}
