package com.nutricion.servicios;

import com.nutricion.entidades.Alergia;
import com.nutricion.entidades.Paciente;
import com.nutricion.entidades.PlanAlimenticio;
import com.nutricion.entidades.PlanTratamiento;
import com.nutricion.entidades.Rol;
import com.nutricion.entidades.Usuario;
import com.nutricion.servicios.CatalogoSmae.Alimento;
import com.nutricion.util.ReglaNegocioException;
import java.util.List;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.CacheStoreMode;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Stateless
public class PlanAlimenticioFacade extends AbstractFacade<PlanAlimenticio> {

    private static final String REFRESCAR = "javax.persistence.cache.storeMode";
    private static final String AMBITO = " WHERE a.tratamiento.paciente.id = :paciente"
            + " AND a.tratamiento.paciente.consultorio.id = :consultorio AND a.fechaAprobacion IS NOT NULL";

    @PersistenceContext(unitName = "NutricionPU")
    private EntityManager em;

    @EJB
    private PacienteFacade pacienteFacade;

    public PlanAlimenticioFacade() { super(PlanAlimenticio.class); }

    @Override
    protected EntityManager getEntityManager() { return em; }

    public PlanAlimenticio generar(Long pacienteId, Long usuarioId) {
        Usuario usuario = usuarioClinico(usuarioId, LockModeType.NONE);
        Paciente paciente = expediente(pacienteId, usuario, LockModeType.NONE);
        PlanTratamiento tratamiento = tratamientoVigente(paciente, LockModeType.NONE);
        return GeneradorPlanes.generar(tratamiento, permitidos(paciente));
    }

    public PlanAlimenticio ultimoAprobado(Long pacienteId, Long usuarioId) {
        Usuario usuario = usuarioClinico(usuarioId, LockModeType.NONE);
        Paciente paciente = expediente(pacienteId, usuario, LockModeType.NONE);
        return cargarPorciones(ultimo(paciente, false));
    }

    public PlanAlimenticio aprobar(Long pacienteId, Long usuarioId, PlanAlimenticio borrador, boolean revisado) {
        if (borrador == null || !revisado) {
            throw new ReglaNegocioException("Revisa el borrador y confirma la revisión antes de aprobar.");
        }
        Usuario usuario = usuarioClinico(usuarioId, LockModeType.PESSIMISTIC_READ);
        Paciente paciente = expediente(pacienteId, usuario, LockModeType.PESSIMISTIC_WRITE);
        PlanTratamiento tratamiento = tratamientoVigente(paciente, LockModeType.PESSIMISTIC_WRITE);
        validarVigente(borrador, tratamiento, paciente);
        PlanAlimenticio anterior = primero(consultaPlanes(paciente, " AND a.folioBorrador = :folio")
                .setParameter("folio", borrador.getFolioBorrador()));
        if (anterior != null) {
            return cargarPorciones(anterior);
        }
        borrador.aprobar(tratamiento, usuario);
        em.persist(borrador);
        return borrador;
    }

    public PlanAlimenticio entregar(Long pacienteId, Long usuarioId, Long planId) {
        Usuario usuario = usuarioClinico(usuarioId, LockModeType.PESSIMISTIC_READ);
        Paciente paciente = expediente(pacienteId, usuario, LockModeType.PESSIMISTIC_WRITE);
        PlanAlimenticio plan = ultimo(paciente, false);
        if (plan == null || !plan.getId().equals(planId)) {
            throw new ReglaNegocioException("Aprueba el borrador vigente antes de entregarlo al portal.");
        }
        if (plan.getFechaEntrega() == null) {
            validarVigente(plan, tratamientoVigente(paciente, LockModeType.PESSIMISTIC_WRITE), paciente);
            plan.entregar();
        }
        return cargarPorciones(plan);
    }

    public PlanAlimenticio entregadoParaCodigo(String codigo) {
        Paciente paciente = pacienteFacade.porCodigoAcceso(codigo);
        if (paciente == null || paciente.getConsultorio() == null) {
            return null;
        }
        em.refresh(paciente);
        PlanAlimenticio plan = ultimo(paciente, true);
        if (plan != null) {
            try {
                GeneradorPlanes.validar(plan, permitidos(paciente));
            } catch (ReglaNegocioException restringido) {
                return null;
            }
        }
        return plan;
    }

    private void validarVigente(PlanAlimenticio plan, PlanTratamiento tratamiento, Paciente paciente) {
        if (tratamiento == null || !plan.coincideCon(tratamiento)) {
            throw new ReglaNegocioException("La meta o los macros guardados cambiaron. Genera otro borrador con el cálculo vigente.");
        }
        GeneradorPlanes.validar(plan, permitidos(paciente));
    }

    private Usuario usuarioClinico(Long id, LockModeType bloqueo) {
        Usuario usuario = id == null ? null : em.find(Usuario.class, id, bloqueo);
        if (usuario != null) {
            em.refresh(usuario, bloqueo);
        }
        if (usuario == null || !usuario.isActivo() || usuario.getConsultorio() == null
                || (usuario.getRol() != Rol.ADMIN && usuario.getRol() != Rol.NUTRIOLOGO)) {
            throw new ReglaNegocioException("Se requiere una cuenta clínica activa para gestionar el plan alimenticio.");
        }
        return usuario;
    }

    private Paciente expediente(Long id, Usuario usuario, LockModeType bloqueo) {
        Paciente paciente = primero(em.createQuery("SELECT p FROM Paciente p WHERE p.id = :id"
                        + " AND p.consultorio = :consultorio", Paciente.class)
                .setParameter("id", id).setParameter("consultorio", usuario.getConsultorio())
                .setLockMode(bloqueo).setHint(REFRESCAR, CacheStoreMode.REFRESH));
        if (paciente == null) {
            throw new ReglaNegocioException("El expediente no pertenece a este consultorio.");
        }
        return paciente;
    }

    private PlanTratamiento tratamientoVigente(Paciente paciente, LockModeType bloqueo) {
        return primero(em.createQuery("SELECT t FROM PlanTratamiento t WHERE t.paciente = :paciente"
                        + " AND t.paciente.consultorio.id = :consultorio ORDER BY t.fecha DESC, t.id DESC", PlanTratamiento.class)
                .setParameter("paciente", paciente).setParameter("consultorio", paciente.getConsultorio().getId())
                .setLockMode(bloqueo).setHint(REFRESCAR, CacheStoreMode.REFRESH));
    }

    private Set<Alimento> permitidos(Paciente paciente) {
        List<Alergia> alergias = em.createQuery("SELECT a FROM Paciente p JOIN p.alergiasCatalogo a "
                        + "WHERE p.id = :paciente AND p.consultorio.id = :consultorio", Alergia.class)
                .setParameter("paciente", paciente.getId()).setParameter("consultorio", paciente.getConsultorio().getId())
                .setHint(REFRESCAR, CacheStoreMode.REFRESH).getResultList();
        return CatalogoSmae.permitidos(paciente, alergias);
    }

    private PlanAlimenticio ultimo(Paciente paciente, boolean entregado) {
        String orden = entregado ? " AND a.fechaEntrega IS NOT NULL ORDER BY a.fechaEntrega DESC, a.id DESC"
                : " ORDER BY a.id DESC";
        return primero(consultaPlanes(paciente, orden));
    }

    private TypedQuery<PlanAlimenticio> consultaPlanes(Paciente paciente, String sufijo) {
        return em.createQuery("SELECT a FROM PlanAlimenticio a" + AMBITO + sufijo, PlanAlimenticio.class)
                .setParameter("paciente", paciente.getId()).setParameter("consultorio", paciente.getConsultorio().getId())
                .setHint(REFRESCAR, CacheStoreMode.REFRESH);
    }

    private static <T> T primero(TypedQuery<T> query) {
        List<T> resultados = query.setMaxResults(1).getResultList();
        return resultados.isEmpty() ? null : resultados.get(0);
    }

    private PlanAlimenticio cargarPorciones(PlanAlimenticio plan) {
        if (plan != null) {
            plan.getPorciones().size();
        }
        return plan;
    }
}
