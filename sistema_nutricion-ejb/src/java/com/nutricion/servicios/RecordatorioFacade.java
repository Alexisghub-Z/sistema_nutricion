package com.nutricion.servicios;

import com.nutricion.entidades.Cita;
import com.nutricion.entidades.Consultorio;
import com.nutricion.entidades.EstadoCita;
import com.nutricion.entidades.Paciente;
import com.nutricion.entidades.Recordatorio;
import com.nutricion.entidades.Rol;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Stateless
public class RecordatorioFacade extends AbstractFacade<Recordatorio> {

    private static final String CITAS_EN_VENTANA = "c.paciente.consultorio = :consultorio "
            + "AND c.paciente.consultorio.activo = true AND c.estado IN :estados "
            + "AND c.fechaHora > :ahora AND c.fechaHora <= :hasta";
    private static final String DEL_NUTRIOLOGO = "r.tipo = com.nutricion.entidades.TipoRecordatorio.NUTRIOLOGO";
    private static final List<EstadoCita> ESTADOS = Arrays.asList(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA);

    @PersistenceContext(unitName = "NutricionPU")
    private EntityManager em;

    @EJB
    private DisponibilidadService disponibilidad;

    public RecordatorioFacade() {
        super(Recordatorio.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public void publicar(Consultorio consultorio) {
        // El mismo bloqueo que usa la agenda serializa la publicación y la reprogramación.
        Consultorio actual = em.find(Consultorio.class, consultorio.getId(), LockModeType.PESSIMISTIC_WRITE);
        if (actual == null || !actual.isActivo()) {
            return;
        }
        Date ahora = new Date();
        int margen = disponibilidad.obtenerConfiguracion(actual).getMinutosRecordatorioNutriologo();
        Date hasta = Fechas.sumarMinutos(ahora, margen);
        List<Cita> citas = enVentana("SELECT c FROM Cita c WHERE ", Cita.class, actual, ahora, hasta)
                .getResultList();
        if (citas.isEmpty()) {
            return;
        }
        Map<Long, Recordatorio> publicados = new HashMap<>();
        enVentana("SELECT r FROM Recordatorio r JOIN r.cita c WHERE " + DEL_NUTRIOLOGO + " AND ",
                Recordatorio.class, actual, ahora, hasta).getResultList()
                .forEach(r -> publicados.put(r.getCita().getId(), r));

        for (Cita cita : citas) {
            Date programado = Fechas.sumarMinutos(cita.getFechaHora(), -margen);
            Recordatorio existente = publicados.get(cita.getId());
            if (existente == null) {
                em.persist(new Recordatorio(cita, programado));
            } else if (!programado.equals(existente.getProgramadoPara())) {
                existente.setProgramadoPara(programado);
            }
        }
    }

    public List<Cita> vigentes(Consultorio consultorio, Rol rol) {
        if (consultorio == null || (rol != Rol.ADMIN && rol != Rol.NUTRIOLOGO)) {
            return Collections.emptyList();
        }
        Date ahora = new Date();
        int margen = disponibilidad.obtenerConfiguracion(consultorio).getMinutosRecordatorioNutriologo();
        return enVentana("SELECT c FROM Recordatorio r JOIN r.cita c WHERE " + DEL_NUTRIOLOGO + " AND ",
                Cita.class, consultorio, ahora, Fechas.sumarMinutos(ahora, margen))
                .getResultList();
    }

    public void eliminarDelPaciente(Paciente paciente) {
        em.createQuery("DELETE FROM Recordatorio r WHERE r.cita IN "
                + "(SELECT c FROM Cita c WHERE c.paciente = :paciente)")
                .setParameter("paciente", paciente).executeUpdate();
    }

    private <T> TypedQuery<T> enVentana(String consulta, Class<T> tipo, Consultorio consultorio, Date ahora, Date hasta) {
        return em.createQuery(consulta + CITAS_EN_VENTANA + " ORDER BY c.fechaHora, c.id", tipo)
                .setParameter("consultorio", consultorio).setParameter("estados", ESTADOS)
                .setParameter("ahora", ahora).setParameter("hasta", hasta);
    }
}
