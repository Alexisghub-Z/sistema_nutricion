package com.nutricion.servicios;

import com.nutricion.entidades.Cita;
import com.nutricion.entidades.Consultorio;
import com.nutricion.entidades.EstadoCita;
import com.nutricion.entidades.OrigenCita;
import com.nutricion.entidades.Paciente;
import com.nutricion.util.ReglaNegocioException;
import com.nutricion.util.Seguridad;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;

@Stateless
public class CitaFacade extends AbstractFacade<Cita> {

    @PersistenceContext(unitName = "NutricionPU")
    private EntityManager em;

    @EJB
    private PacienteFacade pacienteFacade;

    @EJB
    private DisponibilidadService disponibilidadService;

    public CitaFacade() {
        super(Cita.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public List<Cita> delDia(Date dia, Consultorio c) {
        Date desde = Fechas.inicioDelDia(dia);
        return entreFechas(desde, Fechas.sumarDias(desde, 1), c);
    }

    public List<Cita> entreFechas(Date desde, Date hasta, Consultorio c) {
        return em.createQuery(
                "SELECT c FROM Cita c WHERE c.paciente.consultorio = :consultorio AND c.fechaHora >= :desde AND c.fechaHora < :hasta ORDER BY c.fechaHora",
                Cita.class)
                .setParameter("consultorio", c)
                .setParameter("desde", desde)
                .setParameter("hasta", hasta)
                .getResultList();
    }

    public List<Cita> proximas(int max, Consultorio c) {
        return em.createQuery(
                "SELECT c FROM Cita c WHERE c.paciente.consultorio = :consultorio AND c.fechaHora >= :desde "
                + "AND c.estado IN (com.nutricion.entidades.EstadoCita.PENDIENTE, com.nutricion.entidades.EstadoCita.CONFIRMADA, com.nutricion.entidades.EstadoCita.APARTADA) "
                + "ORDER BY c.fechaHora", Cita.class)
                .setParameter("consultorio", c)
                .setParameter("desde", new Date())
                .setMaxResults(max)
                .getResultList();
    }

    public Cita siguienteDePaciente(Paciente paciente) {
        return em.createQuery("SELECT c FROM Cita c WHERE c.paciente = :paciente AND c.fechaHora > :ahora "
                + "AND c.estado IN :estados ORDER BY c.fechaHora", Cita.class)
                .setParameter("paciente", paciente).setParameter("ahora", new Date())
                .setParameter("estados", java.util.Arrays.asList(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA))
                .setMaxResults(1).getResultStream().findFirst().orElse(null);
    }

    public Cita delConsultorio(Long id, Consultorio consultorio) {
        if (id == null || consultorio == null) {
            return null;
        }
        return em.createQuery("SELECT c FROM Cita c WHERE c.id = :id AND c.paciente.consultorio = :consultorio", Cita.class)
                .setParameter("id", id).setParameter("consultorio", consultorio)
                .getResultStream().findFirst().orElse(null);
    }

    public Cita porCodigo(String codigo) {
        if (codigo == null) {
            return null;
        }
        List<Cita> r = em.createNamedQuery("Cita.porCodigo", Cita.class)
                .setParameter("codigo", codigo.trim().toUpperCase(Locale.ROOT)).getResultList();
        return r.isEmpty() ? null : r.get(0);
    }

    public List<ResumenAsistencia> asistenciaEntre(Consultorio consultorio, Date desde, Date hasta) {
        return em.createQuery("SELECT NEW com.nutricion.servicios.ResumenAsistencia(p.nombre, COUNT(c), "
                + "SUM(CASE WHEN c.estado = :asistio THEN 1 ELSE 0 END)) FROM Cita c JOIN c.paciente p "
                + "WHERE p.consultorio = :consultorio AND c.fechaHora >= :desde AND c.fechaHora <= :hasta "
                + "AND c.estado IN (:asistio, :ausente) GROUP BY p.id, p.nombre ORDER BY p.nombre, p.id",
                ResumenAsistencia.class).setParameter("consultorio", consultorio).setParameter("desde", desde)
                .setParameter("hasta", hasta).setParameter("asistio", EstadoCita.COMPLETADA)
                .setParameter("ausente", EstadoCita.NO_ASISTIO).getResultList();
    }

    /** Agenda (o reprograma) una cita validando horario y empalmes. */
    public Cita guardar(Cita c, Consultorio consultorio) {
        bloquearConsultorio(consultorio);
        validarDatosCita(c);
        if (c.getPaciente() == null || pacienteFacade.delConsultorio(c.getPaciente().getId(), consultorio) == null) {
            throw new ReglaNegocioException("Selecciona un paciente de este consultorio.");
        }
        if (c.getId() != null) {
            Cita anterior = delConsultorio(c.getId(), consultorio);
            if (anterior == null || anterior.getConsulta() != null) {
                throw new ReglaNegocioException("No se puede modificar esta cita.");
            }
        }
        disponibilidadService.validarDentroDeHorario(c.getFechaHora(), c.getDuracionMinutos(), consultorio);
        validarEmpalmes(c, c.getEstado(), consultorio);
        if (c.getId() == null) {
            return crearCita(c);
        }
        return em.merge(c);
    }

    private Cita crearCita(Cita cita) {
        cita.setCodigo(codigoUnico());
        em.persist(cita);
        return cita;
    }

    public Cita agendarPublica(Paciente datos, Cita cita, Consultorio consultorio) {
        Consultorio actual = bloquearConsultorio(consultorio);
        if (!actual.isActivo()) {
            throw new ReglaNegocioException("Selecciona un consultorio disponible.");
        }
        cita.setEstado(EstadoCita.PENDIENTE);
        cita.setOrigen(OrigenCita.PORTAL);
        cita.setDuracionMinutos(disponibilidadService.obtenerConfiguracion(actual).getDuracionConsultaMin());
        validarDatosCita(cita);
        if (!disponibilidadService.slotsDisponibles(cita.getFechaHora(), actual).contains(cita.getFechaHora())) {
            throw new ReglaNegocioException("Ese horario ya no está disponible. Elige otra hora.");
        }

        Paciente p = pacienteFacade.porEmail(datos.getEmail(), actual);
        Paciente porTelefono = pacienteFacade.porTelefono(datos.getTelefono(), actual);
        if ((p != null || porTelefono != null) && (p == null || !p.equals(porTelefono))) {
            throw new ReglaNegocioException("No se pudo usar este contacto para reservar. Comunícate con el consultorio.");
        }
        if (p == null) {
            p = pacienteFacade.guardar(datos, actual);
        }
        cita.setPaciente(p);
        return crearCita(cita);
    }

    public void cambiarEstado(Long id, EstadoCita estado, Consultorio consultorio) {
        bloquearConsultorio(consultorio);
        Cita c = delConsultorio(id, consultorio);
        if (c == null || estado == null || c.getConsulta() != null) {
            throw new ReglaNegocioException("No se puede cambiar el estado de esta cita.");
        }
        validarEmpalmes(c, estado, consultorio);
        c.setEstado(estado);
    }

    public void actualizarDesdePortal(Long id, Paciente paciente, EstadoCita estado) {
        Consultorio consultorio = paciente.getConsultorio();
        bloquearConsultorio(consultorio);
        Cita c = delConsultorio(id, consultorio);
        if (c == null || !paciente.equals(c.getPaciente())
                || (estado != EstadoCita.CONFIRMADA && estado != EstadoCita.CANCELADA)) {
            throw new ReglaNegocioException("No se puede actualizar esta cita desde el portal.");
        }
        validarCitaFuturaActiva(c);
        c.setEstado(estado);
    }

    /** Cancelación hecha por el propio paciente con su código. */
    public Cita cancelarPorCodigo(String codigo) {
        Cita c = porCodigo(codigo);
        if (c == null) {
            throw new ReglaNegocioException("No encontramos una cita con ese código.");
        }
        em.refresh(c, LockModeType.PESSIMISTIC_WRITE);
        validarCitaFuturaActiva(c);
        c.setEstado(EstadoCita.CANCELADA);
        return c;
    }

    private void validarCitaFuturaActiva(Cita c) {
        if (!c.getFechaHora().after(new Date()) || !ocupaHorario(c.getEstado()) || c.getConsulta() != null) {
            throw new ReglaNegocioException("Solo se pueden actualizar citas futuras y activas.");
        }
    }

    private Consultorio bloquearConsultorio(Consultorio consultorio) {
        // Una fila ya existente serializa las modificaciones de la agenda de este consultorio.
        if (consultorio == null || consultorio.getId() == null) {
            throw new ReglaNegocioException("Selecciona un consultorio.");
        }
        Consultorio c = em.find(Consultorio.class, consultorio.getId(), LockModeType.PESSIMISTIC_WRITE);
        if (c == null) {
            throw new ReglaNegocioException("Selecciona un consultorio.");
        }
        return c;
    }

    private void validarDatosCita(Cita c) {
        if (c.getFechaHora() == null) {
            throw new ReglaNegocioException("Indica la fecha y hora de la cita.");
        }
        if (c.getMotivo() == null || c.getMotivo().trim().isEmpty() || c.getMotivo().length() > 1000) {
            throw new ReglaNegocioException("Indica el motivo de la cita (hasta 1000 caracteres).");
        }
        if (c.getEstado() == null || c.getTipo() == null) {
            throw new ReglaNegocioException("Selecciona el estado y la modalidad de la cita.");
        }
        if (c.getDuracionMinutos() < 15 || c.getDuracionMinutos() > 180) {
            throw new ReglaNegocioException("La duración debe estar entre 15 y 180 minutos.");
        }
    }

    private boolean ocupaHorario(EstadoCita estado) {
        return estado == EstadoCita.PENDIENTE || estado == EstadoCita.CONFIRMADA || estado == EstadoCita.APARTADA;
    }

    private void validarEmpalmes(Cita c, EstadoCita estado, Consultorio consultorio) {
        if (!ocupaHorario(estado)) {
            return;
        }
        Date fin = Fechas.sumarMinutos(c.getFechaHora(), c.getDuracionMinutos());
        if (!disponibilidadService.bloqueos(c.getFechaHora(), fin, consultorio).isEmpty()) {
            throw new ReglaNegocioException("Ese horario está ocupado en Google Calendar. Elige otra hora.");
        }
        for (Cita o : disponibilidadService.citasQueOcupan(c.getFechaHora(), fin, consultorio)) {
            if (!o.getId().equals(c.getId()) && Fechas.sumarMinutos(o.getFechaHora(), o.getDuracionMinutos()).after(c.getFechaHora())) {
                throw new ReglaNegocioException("Ese horario se empalma con otra cita. Elige otra hora.");
            }
        }
    }

    private String codigoUnico() {
        String codigo;
        do {
            codigo = Seguridad.codigoCita();
        } while (porCodigo(codigo) != null);
        return codigo;
    }
}
