package com.nutricion.servicios;

import com.nutricion.entidades.Consultorio;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ProgramadorRecordatorios {

    private static final Logger LOG = Logger.getLogger(ProgramadorRecordatorios.class.getName());

    @EJB
    private ConsultorioFacade consultorios;

    @EJB
    private RecordatorioFacade recordatorios;

    @Schedule(hour = "*", minute = "*", persistent = false, info = "recordatorios-consulta")
    public void publicar() {
        for (Consultorio consultorio : consultorios.activos()) {
            try {
                recordatorios.publicar(consultorio);
            } catch (RuntimeException e) {
                LOG.warning("No se pudieron preparar los recordatorios del consultorio "
                        + consultorio.getId() + " (" + e.getClass().getSimpleName() + ").");
            }
        }
    }
}
