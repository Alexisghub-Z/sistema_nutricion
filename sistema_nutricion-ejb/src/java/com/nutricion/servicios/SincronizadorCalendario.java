package com.nutricion.servicios;

import com.nutricion.entidades.BloqueoCalendario;
import com.nutricion.entidades.Cita;
import com.nutricion.entidades.Consultorio;
import com.nutricion.entidades.EstadoCita;
import com.nutricion.entidades.VinculoGoogleCalendar;
import com.nutricion.util.ConfiguracionGoogle;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class SincronizadorCalendario {

    private static final Logger LOG = Logger.getLogger(SincronizadorCalendario.class.getName());
    private static final int DIAS_SINCRONIZADOS = 90;

    @EJB
    private CalendarioGoogleFacade calendario;

    @EJB
    private CitaFacade citas;

    public void vincular(Consultorio consultorio, String tokenCifrado) {
        calendario.vincular(consultorio, tokenCifrado);
    }

    public void desvincular(Consultorio consultorio) {
        calendario.desvincular(consultorio);
    }

    @Schedule(hour = "*", minute = "*/5", persistent = false)
    public void sincronizar() {
        ConfiguracionGoogle config = ConfiguracionGoogle.cargar();
        if (config == null) {
            return;
        }
        for (VinculoGoogleCalendar vinculo : calendario.vinculados()) {
            boolean correcto = false;
            try (GoogleCalendarClient google = new GoogleCalendarClient()) {
                sincronizar(vinculo, config, google, Instant.now().truncatedTo(ChronoUnit.SECONDS));
                correcto = true;
            } catch (RuntimeException e) {
                LOG.warning("Calendar no pudo sincronizar el consultorio " + vinculo.getConsultorio().getId()
                        + " (" + e.getClass().getSimpleName() + ").");
            }
            calendario.registrarResultado(vinculo, correcto);
        }
    }

    void sincronizar(VinculoGoogleCalendar vinculo, ConfiguracionGoogle config,
            GoogleCalendarClient google, Instant desde) {
        Consultorio consultorio = vinculo.getConsultorio();
        String marca = vinculo.getMarca();
        String prefijoConsultorio = "nv" + marca + "v";
        Instant hasta = desde.plus(DIAS_SINCRONIZADOS, ChronoUnit.DAYS);
        String autorizado = config.descifrar(vinculo.getTokenCifrado(), consultorio.getId());
        String token = google.refrescar(config, autorizado);
        List<BloqueoCalendario> bloqueos = new ArrayList<>();
        List<GoogleCalendarClient.Evento> propios = new ArrayList<>();
        for (GoogleCalendarClient.Evento evento : google.eventos(token, desde, hasta)) {
            if (marca.equals(evento.marca) && evento.id.startsWith(prefijoConsultorio)) {
                propios.add(evento);
            } else if (evento.ocupado) {
                bloqueos.add(new BloqueoCalendario(consultorio, Date.from(evento.inicio), Date.from(evento.fin)));
            }
        }

        if (!calendario.guardarBloqueos(vinculo, bloqueos)) {
            return;
        }
        for (Cita cita : citas.entreFechas(Date.from(desde), Date.from(hasta), consultorio)) {
            if (cita.getEstado() != EstadoCita.PENDIENTE && cita.getEstado() != EstadoCita.CONFIRMADA
                    && cita.getEstado() != EstadoCita.APARTADA) {
                continue;
            }
            String prefijo = prefijoConsultorio + Long.toHexString(cita.getId()) + "v";
            String id = prefijo + Long.toHexString(cita.getVersion() == null ? 0 : cita.getVersion());
            GoogleCalendarClient.Evento existente = propios.stream().filter(ev -> ev.id.startsWith(prefijo))
                    .findFirst().orElse(null);
            google.publicar(token, id, marca, cita.getFechaHora().toInstant(),
                    Fechas.sumarMinutos(cita.getFechaHora(), cita.getDuracionMinutos()).toInstant(), existente);
            propios.remove(existente);
        }
        for (GoogleCalendarClient.Evento obsoleto : propios) {
            if (!obsoleto.inicio.isBefore(desde)) {
                google.cancelar(token, obsoleto.id);
            }
        }
    }
}
