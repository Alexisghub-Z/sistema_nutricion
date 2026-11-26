package com.nutricion.web.beans;

import com.nutricion.entidades.Cita;
import com.nutricion.servicios.RecordatorioFacade;
import java.io.Serializable;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.context.RequestContext;

@Named
@ViewScoped
public class RecordatoriosBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private RecordatorioFacade recordatorios;

    @Inject
    private LoginBean login;

    private List<Cita> avisos;

    @PostConstruct
    public void cargar() {
        avisos = consultar();
    }

    public void actualizar() {
        List<Cita> actuales = consultar();
        if (!mismosAvisos(actuales)) {
            avisos = actuales;
            RequestContext.getCurrentInstance().update("frmRecordatorios:avisos");
        }
    }

    private List<Cita> consultar() {
        return recordatorios.vigentes(login.getConsultorioActivo(), login.getUsuario().getRol());
    }

    private boolean mismosAvisos(List<Cita> actuales) {
        if (avisos.size() != actuales.size()) {
            return false;
        }
        for (int i = 0; i < avisos.size(); i++) {
            Cita anterior = avisos.get(i);
            Cita actual = actuales.get(i);
            if (!anterior.equals(actual) || !anterior.getFechaHora().equals(actual.getFechaHora())
                    || !anterior.getPaciente().getNombre().equals(actual.getPaciente().getNombre())
                    || !anterior.getMotivo().equals(actual.getMotivo())) {
                return false;
            }
        }
        return true;
    }

    public List<Cita> getAvisos() { return avisos; }
}
