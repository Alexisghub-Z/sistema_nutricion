package com.nutricion.servicios;

import com.nutricion.entidades.Cita;
import com.nutricion.entidades.Consulta;
import com.nutricion.entidades.EstadoCita;
import com.nutricion.entidades.Paciente;
import com.nutricion.util.ReglaNegocioException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class ConsultaFacade extends AbstractFacade<Consulta> {

    @PersistenceContext(unitName = "NutricionPU")
    private EntityManager em;

    public ConsultaFacade() {
        super(Consulta.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    /** Historial de un paciente, más reciente primero. */
    public List<Consulta> historial(Paciente p) {
        return em.createNamedQuery("Consulta.porPaciente", Consulta.class).setParameter("paciente", p).getResultList();
    }

    /** Historial en orden cronológico (para la gráfica de evolución). */
    public List<Consulta> evolucion(Paciente p) {
        return em.createNamedQuery("Consulta.porPacienteAsc", Consulta.class).setParameter("paciente", p).getResultList();
    }

    public List<Consulta> recientes(int max) {
        return em.createNamedQuery("Consulta.recientes", Consulta.class).setMaxResults(max).getResultList();
    }

    /** Registra o edita la consulta; si viene de una cita la marca como completada. */
    public Consulta guardar(Consulta c) {
        validar(c);
        Consulta guardada;
        if (c.getId() == null) {
            em.persist(c);
            guardada = c;
        } else {
            guardada = em.merge(c);
        }
        if (guardada.getCita() != null) {
            Cita cita = em.find(Cita.class, guardada.getCita().getId());
            cita.setEstado(EstadoCita.COMPLETADA);
            cita.setConsulta(guardada);
        }
        return guardada;
    }

    public void eliminar(Long id) {
        Consulta c = em.find(Consulta.class, id);
        if (c != null) {
            if (c.getCita() != null) {
                c.getCita().setConsulta(null);
            }
            em.remove(c);
        }
    }

    public long consultasDelMes() {
        Date hoy = new Date();
        return em.createNamedQuery("Consulta.contarEntre", Long.class)
                .setParameter("desde", Fechas.inicioDelMes(hoy))
                .setParameter("hasta", Fechas.inicioMesSiguiente(hoy))
                .getSingleResult();
    }

    public BigDecimal ingresosDelMes() {
        Date hoy = new Date();
        BigDecimal r = em.createNamedQuery("Consulta.ingresosEntre", BigDecimal.class)
                .setParameter("desde", Fechas.inicioDelMes(hoy))
                .setParameter("hasta", Fechas.inicioMesSiguiente(hoy))
                .getSingleResult();
        return r == null ? BigDecimal.ZERO : r;
    }

    private void validar(Consulta c) {
        if (c.getPaciente() == null) {
            throw new ReglaNegocioException("La consulta debe pertenecer a un paciente.");
        }
        if (c.getFecha() != null && c.getFecha().after(Fechas.sumarDias(new Date(), 1))) {
            throw new ReglaNegocioException("La fecha de la consulta no puede ser futura.");
        }
        if (c.getPeso() != null && (c.getPeso() < 2 || c.getPeso() > 400)) {
            throw new ReglaNegocioException("El peso debe estar entre 2 y 400 kg.");
        }
        if (c.getTalla() != null && (c.getTalla() < 0.4 || c.getTalla() > 2.5)) {
            throw new ReglaNegocioException("La talla va en metros (ej. 1.65).");
        }
        if (c.getGrasaCorporal() != null && (c.getGrasaCorporal() < 1 || c.getGrasaCorporal() > 75)) {
            throw new ReglaNegocioException("El porcentaje de grasa debe estar entre 1 y 75.");
        }
        if (c.getMontoConsulta() != null && c.getMontoConsulta().signum() < 0) {
            throw new ReglaNegocioException("El monto no puede ser negativo.");
        }
    }
}
