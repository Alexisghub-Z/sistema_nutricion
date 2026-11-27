package com.nutricion.servicios;

import com.nutricion.entidades.Cita;
import com.nutricion.entidades.Consulta;
import com.nutricion.entidades.Consultorio;
import com.nutricion.entidades.EstadoCita;
import com.nutricion.entidades.EstadoPago;
import com.nutricion.entidades.MetodoPago;
import com.nutricion.entidades.Paciente;
import com.nutricion.util.ReglaNegocioException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

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

    public List<Consulta> medicionesParaPortal(Paciente paciente) {
        return em.createQuery("SELECT c FROM Consulta c LEFT JOIN c.cita cita "
                + "WHERE c.paciente = :paciente AND (c.cerrada = true OR cita.estado = :completada) "
                + "AND c.peso IS NOT NULL ORDER BY c.fecha, c.id", Consulta.class)
                .setParameter("paciente", paciente).setParameter("completada", EstadoCita.COMPLETADA)
                .getResultList();
    }

    public Consulta detalle(Long id, Paciente paciente) {
        return em.createQuery("SELECT c FROM Consulta c WHERE c.id = :id AND c.paciente = :paciente", Consulta.class)
                .setParameter("id", id).setParameter("paciente", paciente)
                .getResultStream().findFirst().orElse(null);
    }

    /** Historial en orden cronológico filtrado por rango de fechas (HU-09). */
    public List<Consulta> evolucion(Paciente p, Date desde, Date hasta) {
        StringBuilder jpql = new StringBuilder("SELECT c FROM Consulta c WHERE c.paciente = :paciente");
        if (desde != null) {
            jpql.append(" AND c.fecha >= :desde");
        }
        if (hasta != null) {
            jpql.append(" AND c.fecha < :hasta");
        }
        jpql.append(" ORDER BY c.fecha, c.id");
        javax.persistence.TypedQuery<Consulta> q = em.createQuery(jpql.toString(), Consulta.class)
                .setParameter("paciente", p);
        if (desde != null) {
            q.setParameter("desde", Fechas.inicioDelDia(desde));
        }
        if (hasta != null) {
            q.setParameter("hasta", Fechas.sumarDias(Fechas.inicioDelDia(hasta), 1));
        }
        return q.getResultList();
    }

    public List<Consulta> recientes(int max, Consultorio c) {
        return em.createQuery("SELECT c FROM Consulta c WHERE c.paciente.consultorio = :consultorio ORDER BY c.fecha DESC",
                Consulta.class)
                .setParameter("consultorio", c)
                .setMaxResults(max)
                .getResultList();
    }

    /** Registra o edita la consulta, con opción de cerrarla en solo lectura. */
    public Consulta guardar(Consulta c, boolean cerrar, Consultorio consultorio) {
        Consulta existente = validar(c, consultorio);
        if (tienePago(c)) {
            c.setFechaPago(existente != null && tienePago(existente) ? existente.getFechaPago() : new Date());
        } else {
            c.setFechaPago(null);
        }
        if (cerrar) {
            c.setCerrada(true);
            if (c.getFechaCierre() == null) {
                c.setFechaCierre(new Date());
            }
        }
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

    public long consultasDelMes(Consultorio c) {
        Date hoy = new Date();
        return em.createQuery(
                "SELECT COUNT(c) FROM Consulta c WHERE c.paciente.consultorio = :consultorio AND c.fecha >= :desde AND c.fecha < :hasta",
                Long.class)
                .setParameter("consultorio", c)
                .setParameter("desde", Fechas.inicioDelMes(hoy))
                .setParameter("hasta", Fechas.inicioMesSiguiente(hoy))
                .getSingleResult();
    }

    public BigDecimal ingresosDelMes(Consultorio c) {
        Date hoy = new Date();
        return ingresosEntre(c, Fechas.inicioDelMes(hoy), Fechas.inicioMesSiguiente(hoy));
    }

    public BigDecimal ingresosEntre(Consultorio c, Date desde, Date hasta) {
        BigDecimal r = em.createQuery(
                "SELECT SUM(c.montoConsulta) FROM Consulta c WHERE c.paciente.consultorio = :consultorio "
                + "AND c.estadoPago = :estado "
                + "AND ((c.fechaPago >= :desde AND c.fechaPago < :hasta) "
                + "OR (c.fechaPago IS NULL AND c.fecha >= :desde AND c.fecha < :hasta))",
                BigDecimal.class)
                .setParameter("consultorio", c)
                .setParameter("desde", desde)
                .setParameter("hasta", hasta)
                .setParameter("estado", EstadoPago.PAGADO)
                .getSingleResult();
        return r == null ? BigDecimal.ZERO : r;
    }

    public long contarPacientesActivos(Consultorio consultorio, Date ahora, int dias) {
        return em.createQuery("SELECT COUNT(DISTINCT c.paciente.id) FROM Consulta c LEFT JOIN c.cita cita "
                + "WHERE c.paciente.consultorio = :consultorio AND (c.cerrada = true OR cita.estado = :completada) "
                + "AND c.fecha >= :desde AND c.fecha <= :ahora", Long.class)
                .setParameter("consultorio", consultorio)
                .setParameter("completada", EstadoCita.COMPLETADA)
                .setParameter("desde", Fechas.sumarDias(Fechas.inicioDelDia(ahora), 1 - dias))
                .setParameter("ahora", ahora)
                .getSingleResult();
    }

    public List<PagoPendiente> pagosPendientes(Consultorio consultorio, String paciente, Date desde, Date hasta) {
        Date inicio = desde == null ? null : Fechas.inicioDelDia(desde);
        Date fin = hasta == null ? null : Fechas.inicioDelDia(hasta);
        String nombre = paciente == null ? "" : paciente.trim().toLowerCase(Locale.ROOT);
        if (inicio != null && fin != null && inicio.after(fin)) {
            throw new ReglaNegocioException("La fecha inicial debe ser anterior o igual a la final.");
        }
        String jpql = "SELECT NEW com.nutricion.servicios.PagoPendiente(c.id, c.paciente.nombre, c.fecha, "
                + "c.montoConsulta, c.estadoPago) FROM Consulta c WHERE c.paciente.consultorio = :consultorio "
                + "AND c.cerrada = true AND c.estadoPago <> :pagado AND c.montoConsulta > 0";
        if (!nombre.isEmpty()) jpql += " AND LOCATE(:nombre, LOWER(c.paciente.nombre)) > 0";
        if (inicio != null) jpql += " AND c.fecha >= :desde";
        if (fin != null) jpql += " AND c.fecha < :hasta";
        TypedQuery<PagoPendiente> consulta = em.createQuery(jpql + " ORDER BY c.fecha, c.id", PagoPendiente.class)
                .setParameter("consultorio", consultorio).setParameter("pagado", EstadoPago.PAGADO);
        if (!nombre.isEmpty()) consulta.setParameter("nombre", nombre);
        if (inicio != null) consulta.setParameter("desde", inicio);
        if (fin != null) consulta.setParameter("hasta", Fechas.sumarDias(fin, 1));
        return consulta.getResultList();
    }

    public boolean liquidarPago(Long id, Consultorio consultorio, MetodoPago metodo) {
        if (id == null || consultorio == null || metodo == null) {
            throw new ReglaNegocioException("Selecciona un pago y su método de cobro.");
        }
        Consulta consulta = em.createQuery("SELECT c FROM Consulta c WHERE c.id = :id "
                + "AND c.paciente.consultorio = :consultorio AND c.cerrada = true AND c.montoConsulta > 0",
                Consulta.class).setParameter("id", id).setParameter("consultorio", consultorio)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultStream().findFirst().orElse(null);
        if (consulta == null) {
            throw new ReglaNegocioException("El pago no está disponible en este consultorio.");
        }
        em.refresh(consulta, LockModeType.PESSIMISTIC_WRITE);
        if (consulta.getEstadoPago() == EstadoPago.PAGADO) return false;
        consulta.setEstadoPago(EstadoPago.PAGADO);
        consulta.setMetodoPago(metodo);
        consulta.setFechaPago(new Date());
        return true;
    }

    private boolean tienePago(Consulta consulta) {
        return consulta.getEstadoPago() == EstadoPago.PAGADO && consulta.getMontoConsulta() != null
                && consulta.getMontoConsulta().signum() > 0;
    }

    private Consulta validar(Consulta c, Consultorio consultorio) {
        Paciente paciente = c == null || c.getPaciente() == null || c.getPaciente().getId() == null
                ? null : em.find(Paciente.class, c.getPaciente().getId());
        if (paciente == null || consultorio == null || !consultorio.equals(paciente.getConsultorio())) {
            throw new ReglaNegocioException("La consulta debe pertenecer a un paciente de este consultorio.");
        }
        Consulta existente = null;
        if (c.getId() != null) {
            existente = em.find(Consulta.class, c.getId(), LockModeType.PESSIMISTIC_WRITE);
            if (existente == null || !paciente.equals(existente.getPaciente())) {
                throw new ReglaNegocioException("La consulta no pertenece a este paciente.");
            }
            if (existente.isCerrada()) {
                throw new ReglaNegocioException("La consulta ya está cerrada y es de solo lectura.");
            }
        }
        if (c.getCita() != null) {
            Cita cita = em.find(Cita.class, c.getCita().getId());
            if (cita == null || !paciente.equals(cita.getPaciente())) {
                throw new ReglaNegocioException("La cita no pertenece a este paciente.");
            }
        }
        if (c.getFecha() != null && c.getFecha().after(new Date())) {
            throw new ReglaNegocioException("La fecha de la consulta no puede ser futura.");
        }
        if (fueraDeRango(c.getPeso(), 2, 400)) {
            throw new ReglaNegocioException("El peso debe estar entre 2 y 400 kg.");
        }
        if (fueraDeRango(c.getTalla(), 0.4, 2.5)) {
            throw new ReglaNegocioException("La talla debe estar entre 0.40 y 2.50 metros (ej. 1.65).");
        }
        if (fueraDeRango(c.getGrasaCorporal(), 1, 75)) {
            throw new ReglaNegocioException("El porcentaje de grasa debe estar entre 1% y 75%.");
        }
        if (fueraDeRango(c.getMasaMuscular(), 1, 200)) {
            throw new ReglaNegocioException("La masa muscular debe estar entre 1 y 200 kg.");
        }
        if (fueraDeRango(c.getGrasaVisceral(), 1, 30)) {
            throw new ReglaNegocioException("La grasa visceral debe estar entre nivel 1 y 30.");
        }
        if (fueraDeRango(c.getPorcentajeAgua(), 10, 85)) {
            throw new ReglaNegocioException("El porcentaje de agua debe estar entre 10% y 85%.");
        }
        if (fueraDeRango(c.getCintura(), 30, 250)) {
            throw new ReglaNegocioException("La cintura debe estar entre 30 y 250 cm.");
        }
        if (fueraDeRango(c.getCadera(), 30, 250)) {
            throw new ReglaNegocioException("La cadera debe estar entre 30 y 250 cm.");
        }
        if (c.getMontoConsulta() != null && c.getMontoConsulta().signum() < 0) {
            throw new ReglaNegocioException("El monto no puede ser negativo.");
        }
        if (c.getMontoConsulta() != null && (c.getMontoConsulta().compareTo(new BigDecimal("99999999.99")) > 0
                || c.getMontoConsulta().stripTrailingZeros().scale() > 2)) {
            throw new ReglaNegocioException("Escribe un monto de hasta 99,999,999.99 con dos decimales como máximo.");
        }
        if (c.getEstadoPago() == null) {
            throw new ReglaNegocioException("Selecciona el estado de pago.");
        }
        return existente;
    }

    private boolean fueraDeRango(Double valor, double minimo, double maximo) {
        return valor != null && (!Double.isFinite(valor) || valor < minimo || valor > maximo);
    }
}
