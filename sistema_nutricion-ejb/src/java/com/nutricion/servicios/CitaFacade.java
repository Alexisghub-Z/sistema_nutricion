package com.nutricion.servicios;

import com.nutricion.entidades.Cita;
import com.nutricion.entidades.EstadoCita;
import com.nutricion.entidades.Paciente;
import com.nutricion.util.ReglaNegocioException;
import com.nutricion.util.Seguridad;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class CitaFacade extends AbstractFacade<Cita> {

    /** Horario del consultorio. */
    public static final int HORA_APERTURA = 9;
    public static final int HORA_CIERRE = 19;

    @PersistenceContext(unitName = "NutricionPU")
    private EntityManager em;

    @EJB
    private PacienteFacade pacienteFacade;

    public CitaFacade() {
        super(Cita.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public List<Cita> listar(EstadoCita estado) {
        if (estado == null) {
            return em.createNamedQuery("Cita.todas", Cita.class).getResultList();
        }
        return em.createNamedQuery("Cita.porEstado", Cita.class).setParameter("estado", estado).getResultList();
    }

    public List<Cita> delDia(Date dia) {
        Date desde = Fechas.inicioDelDia(dia);
        return em.createNamedQuery("Cita.entreFechas", Cita.class)
                .setParameter("desde", desde)
                .setParameter("hasta", Fechas.sumarDias(desde, 1))
                .getResultList();
    }

    public List<Cita> proximas(int max) {
        return em.createNamedQuery("Cita.proximas", Cita.class)
                .setParameter("desde", new Date())
                .setMaxResults(max)
                .getResultList();
    }

    public List<Cita> dePaciente(Paciente p) {
        return em.createNamedQuery("Cita.porPaciente", Cita.class).setParameter("paciente", p).getResultList();
    }

    public Cita porCodigo(String codigo) {
        if (codigo == null) {
            return null;
        }
        List<Cita> r = em.createNamedQuery("Cita.porCodigo", Cita.class)
                .setParameter("codigo", codigo.trim().toUpperCase()).getResultList();
        return r.isEmpty() ? null : r.get(0);
    }

    /** Agenda (o reprograma) una cita validando horario y empalmes. */
    public Cita guardar(Cita c) {
        validarHorario(c);
        if (c.getId() == null) {
            c.setCodigo(codigoUnico());
            em.persist(c);
            return c;
        }
        return em.merge(c);
    }

    /**
     * Agenda desde la página pública: si el paciente ya existe (por correo o
     * teléfono) se reutiliza; si no, se registra.
     */
    public Cita agendarPublica(Paciente datos, Cita cita) {
        Paciente p = pacienteFacade.porEmail(datos.getEmail());
        if (p == null) {
            p = pacienteFacade.porTelefono(datos.getTelefono());
        }
        if (p == null) {
            p = pacienteFacade.guardar(datos);
        }
        cita.setPaciente(p);
        cita.setEstado(EstadoCita.PENDIENTE);
        if (cita.getFechaHora() != null && cita.getFechaHora().before(new Date())) {
            throw new ReglaNegocioException("Elige una fecha y hora futura.");
        }
        return guardar(cita);
    }

    public Cita cambiarEstado(Long id, EstadoCita estado) {
        Cita c = em.find(Cita.class, id);
        if (c == null) {
            throw new ReglaNegocioException("La cita ya no existe.");
        }
        c.setEstado(estado);
        return c;
    }

    /** Cancelación hecha por el propio paciente con su código. */
    public Cita cancelarPorCodigo(String codigo) {
        Cita c = porCodigo(codigo);
        if (c == null) {
            throw new ReglaNegocioException("No encontramos una cita con ese código.");
        }
        if (c.getEstado() != EstadoCita.PENDIENTE) {
            throw new ReglaNegocioException("Solo se pueden cancelar citas pendientes.");
        }
        c.setEstado(EstadoCita.CANCELADA);
        return c;
    }

    public void eliminar(Long id) {
        Cita c = em.find(Cita.class, id);
        if (c != null) {
            if (c.getConsulta() != null) {
                c.getConsulta().setCita(null);
            }
            em.remove(c);
        }
    }

    private void validarHorario(Cita c) {
        if (c.getFechaHora() == null) {
            throw new ReglaNegocioException("Indica la fecha y hora de la cita.");
        }
        if (c.getDuracionMinutos() < 15 || c.getDuracionMinutos() > 180) {
            throw new ReglaNegocioException("La duración debe estar entre 15 y 180 minutos.");
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(c.getFechaHora());
        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            throw new ReglaNegocioException("El consultorio no abre en domingo.");
        }
        Date fin = Fechas.sumarMinutos(c.getFechaHora(), c.getDuracionMinutos());
        Calendar cierre = Calendar.getInstance();
        cierre.setTime(Fechas.inicioDelDia(c.getFechaHora()));
        cierre.set(Calendar.HOUR_OF_DAY, HORA_CIERRE);
        if (cal.get(Calendar.HOUR_OF_DAY) < HORA_APERTURA || fin.after(cierre.getTime())) {
            throw new ReglaNegocioException("El horario de atención es de " + HORA_APERTURA + ":00 a " + HORA_CIERRE + ":00.");
        }
        if (c.getEstado() == EstadoCita.PENDIENTE) {
            // Busca citas pendientes que empiecen dentro de la ventana [inicio - 3h, fin) y revisa el empalme real.
            List<Cita> cercanas = em.createNamedQuery("Cita.choques", Cita.class)
                    .setParameter("desde", Fechas.sumarMinutos(c.getFechaHora(), -180))
                    .setParameter("hasta", fin)
                    .getResultList();
            for (Cita o : cercanas) {
                if (o.getId().equals(c.getId())) {
                    continue;
                }
                Date finOtra = Fechas.sumarMinutos(o.getFechaHora(), o.getDuracionMinutos());
                if (finOtra.after(c.getFechaHora())) {
                    throw new ReglaNegocioException("Ese horario se empalma con otra cita. Elige otra hora.");
                }
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
