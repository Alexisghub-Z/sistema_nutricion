package com.nutricion.servicios;

import com.nutricion.entidades.Cita;
import com.nutricion.entidades.BloqueoCalendario;
import com.nutricion.entidades.ConfiguracionAgenda;
import com.nutricion.entidades.Consultorio;
import com.nutricion.entidades.HorarioLaboral;
import com.nutricion.util.ReglaNegocioException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;

@Stateless
public class DisponibilidadService {

    @PersistenceContext(unitName = "NutricionPU")
    private EntityManager em;

    public ConfiguracionAgenda obtenerConfiguracion(Consultorio consultorio) {
        return em.createQuery("SELECT c FROM ConfiguracionAgenda c WHERE c.consultorio = :consultorio ORDER BY c.id",
                ConfiguracionAgenda.class)
                .setParameter("consultorio", consultorio).setMaxResults(1)
                .getResultStream().findFirst().orElseGet(ConfiguracionAgenda::new);
    }

    public void guardar(Consultorio consultorio, ConfiguracionAgenda cfg, List<HorarioLaboral> horarios) {
        if (cfg == null) {
            throw new ReglaNegocioException("Configuración inválida.");
        }
        if (cfg.getDuracionConsultaMin() < 15 || cfg.getDuracionConsultaMin() > 180) {
            throw new ReglaNegocioException("La duración de consulta debe estar entre 15 y 180 minutos.");
        }
        if (cfg.getAnticipacionMinimaHoras() < 0 || cfg.getAnticipacionMinimaHoras() > 168) {
            throw new ReglaNegocioException("La anticipación mínima debe estar entre 0 y 168 horas.");
        }
        if (cfg.getDiasPacienteActivo() < 7 || cfg.getDiasPacienteActivo() > 365) {
            throw new ReglaNegocioException("Los días para paciente activo deben estar entre 7 y 365.");
        }
        if (cfg.getMinutosExpiracionApartado() < 1 || cfg.getMinutosExpiracionApartado() > 1440) {
            throw new ReglaNegocioException("La expiración debe estar entre 1 y 1440 minutos.");
        }
        if (consultorio == null || em.find(Consultorio.class, consultorio.getId(), LockModeType.PESSIMISTIC_WRITE) == null) {
            throw new ReglaNegocioException("Selecciona un consultorio.");
        }
        ConfiguracionAgenda actual = obtenerConfiguracion(consultorio);
        actual.setDuracionConsultaMin(cfg.getDuracionConsultaMin());
        actual.setAnticipacionMinimaHoras(cfg.getAnticipacionMinimaHoras());
        actual.setMinutosExpiracionApartado(cfg.getMinutosExpiracionApartado());
        actual.setDiasPacienteActivo(cfg.getDiasPacienteActivo());
        actual.setConsultorio(consultorio);
        if (actual.getId() == null) {
            em.persist(actual);
        }
        for (HorarioLaboral h : horarios) {
            int ini = aMinutos(h.getHoraInicio());
            int fin = aMinutos(h.getHoraFin());
            if (h.getDiaSemana() < 1 || h.getDiaSemana() > 7 || ini < 0 || fin <= ini) {
                throw new ReglaNegocioException("Horario inválido para " + h.getNombreDia()
                        + " (usa formato HH:mm y una hora de fin posterior).");
            }
            HorarioLaboral actualDia = horarioDelDia(h.getDiaSemana(), consultorio);
            actualDia.setHoraInicio(h.getHoraInicio());
            actualDia.setHoraFin(h.getHoraFin());
            actualDia.setActivo(h.isActivo());
            if (actualDia.getId() == null) {
                actualDia.setConsultorio(consultorio);
                em.persist(actualDia);
            }
        }
    }

    public List<HorarioLaboral> listarHorarios(Consultorio consultorio) {
        List<HorarioLaboral> guardados = em.createNamedQuery("HorarioLaboral.todos", HorarioLaboral.class)
                .setParameter("consultorio", consultorio).getResultList();
        List<HorarioLaboral> semana = new ArrayList<>(7);
        for (int dia = 1; dia <= 7; dia++) {
            semana.add(horarioPredeterminado(dia));
        }
        for (HorarioLaboral h : guardados) {
            semana.set(h.getDiaSemana() - 1, h);
        }
        return semana;
    }

    private HorarioLaboral horarioDelDia(int dia, Consultorio consultorio) {
        return em.createNamedQuery("HorarioLaboral.porDia", HorarioLaboral.class)
                .setParameter("dia", dia).setParameter("consultorio", consultorio).setMaxResults(1)
                .getResultStream().findFirst().orElseGet(() -> horarioPredeterminado(dia));
    }

    private HorarioLaboral horarioPredeterminado(int dia) {
        return new HorarioLaboral(dia, "09:00", "19:00", dia != 7);
    }

    public void validarDentroDeHorario(Date inicio, int duracionMin, Consultorio consultorio) {
        HorarioLaboral h = horarioDelDia(diaSemanaIso(inicio), consultorio);
        if (!h.isActivo()) {
            throw new ReglaNegocioException("El consultorio no labora ese día de la semana.");
        }
        int iniLab = aMinutos(h.getHoraInicio());
        int finLab = aMinutos(h.getHoraFin());
        Calendar cal = Calendar.getInstance();
        cal.setTime(inicio);
        int iniCita = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
        int finCita = iniCita + duracionMin;
        if (iniCita < iniLab || finCita > finLab) {
            throw new ReglaNegocioException("El horario de atención para " + h.getNombreDia()
                    + " es de " + h.getHoraInicio() + " a " + h.getHoraFin() + ".");
        }
    }

    /** Calcula los horarios libres reales de un día para reservación en línea (HU-15, HU-19). */
    // Las lecturas de disponibilidad y las escrituras usan las mismas citas activas.
    public List<Cita> citasQueOcupan(Date desde, Date hasta, Consultorio consultorio) {
        return em.createNamedQuery("Cita.choques", Cita.class)
                .setParameter("desde", Fechas.sumarMinutos(desde, -180)).setParameter("hasta", hasta)
                .setParameter("consultorio", consultorio).getResultList();
    }

    public List<BloqueoCalendario> bloqueos(Date desde, Date hasta, Consultorio consultorio) {
        return em.createQuery("SELECT b FROM BloqueoCalendario b WHERE b.consultorio = :consultorio "
                + "AND b.inicio < :hasta AND b.fin > :desde ORDER BY b.inicio", BloqueoCalendario.class)
                .setParameter("consultorio", consultorio).setParameter("desde", desde)
                .setParameter("hasta", hasta).getResultList();
    }

    public List<Date> slotsDisponibles(Date dia, Consultorio consultorio) {
        List<Date> libres = new ArrayList<>();
        if (dia == null || consultorio == null || !consultorio.isActivo()) {
            return libres;
        }
        HorarioLaboral h = horarioDelDia(diaSemanaIso(dia), consultorio);
        if (!h.isActivo()) {
            return libres;
        }
        ConfiguracionAgenda cfg = obtenerConfiguracion(consultorio);
        int duracion = cfg.getDuracionConsultaMin();
        int iniLab = aMinutos(h.getHoraInicio());
        int finLab = aMinutos(h.getHoraFin());

        Date inicioDia = Fechas.inicioDelDia(dia);
        Date finDia = Fechas.sumarDias(inicioDia, 1);
        List<Cita> ocupadas = citasQueOcupan(inicioDia, finDia, consultorio);
        List<BloqueoCalendario> bloqueos = bloqueos(inicioDia, finDia, consultorio);

        Date limiteMinimo = Fechas.sumarMinutos(new Date(), cfg.getAnticipacionMinimaHoras() * 60);

        for (int min = iniLab; min + duracion <= finLab; min += duracion) {
            Date slotInicio = Fechas.sumarMinutos(inicioDia, min);
            Date slotFin = Fechas.sumarMinutos(slotInicio, duracion);
            if (slotInicio.before(limiteMinimo)) {
                continue;
            }
            boolean choca = false;
            for (Cita c : ocupadas) {
                Date cInicio = c.getFechaHora();
                Date cFin = Fechas.sumarMinutos(cInicio, c.getDuracionMinutos());
                if (slotInicio.before(cFin) && slotFin.after(cInicio)) {
                    choca = true;
                    break;
                }
            }
            if (!choca && bloqueos.stream().noneMatch(bloqueo -> bloqueo.coincide(slotInicio, slotFin))) {
                libres.add(slotInicio);
            }
        }
        return libres;
    }

    private static int diaSemanaIso(Date fecha) {
        Calendar c = Calendar.getInstance();
        c.setTime(fecha);
        int dow = c.get(Calendar.DAY_OF_WEEK); // 1=Dom, 2=Lun ... 7=Sab
        return dow == Calendar.SUNDAY ? 7 : dow - 1;
    }

    private static int aMinutos(String hhmm) {
        if (hhmm == null || !hhmm.matches("\\d{2}:\\d{2}")) {
            return -1;
        }
        String[] p = hhmm.split(":");
        int h = Integer.parseInt(p[0]);
        int m = Integer.parseInt(p[1]);
        if (h < 0 || h > 23 || m < 0 || m > 59) {
            return -1;
        }
        return h * 60 + m;
    }
}
